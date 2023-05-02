#!/usr/bin/env python3

import collections.abc
import concurrent.futures
import datetime
import difflib
import json
import os
import sys
import tempfile
import urllib.parse

import click
import httpx
import humanize
from tabulate import tabulate
from jinja2 import Environment, FileSystemLoader, select_autoescape

import api_stats

PROD_URL = "api.wellcomecollection.org"
STAGING_URL = "api-stage.wellcomecollection.org"


class ApiDiffer:
    """Performs a diff against the same call to both prod and stage works API,
    printing the results to stdout.
    """

    def __init__(self, path=None, params=None, **kwargs):
        self.path = f"/catalogue/v2{path}"
        self.params = params or {}

    @staticmethod
    def normalise_absolute_urls(json):
        """
        Finds environment-dependent URLs (eg for @context and pagination) and makes
        them take a default form
        """

        # From https://stackoverflow.com/a/3233356
        def _normalise(data, remaining):
            for key, val in remaining.items():
                if isinstance(val, collections.abc.Mapping):
                    data[key] = _normalise(data.get(key, {}), val)
                elif isinstance(val, str):
                    data[key] = val.replace("api-stage", "api")
                else:
                    data[key] = val
            return data

        return _normalise({}, json)

    @property
    def display_url(self):
        display_params = urllib.parse.urlencode(list(self.params.items()))
        if display_params:
            return f"{self.path}?{display_params}"
        else:
            return self.path

    def get_html_diff(self):
        """
        Fetches a URL from the prod/staging API, and returns a (status, HTML diff).

        The possible statuses are:

            * different status = as in HTTP status
            * match = same JSON
            * different result count = everything is the same except totalResults/totalPages
            * different JSON = something is different

        """

        (prod_status, prod_json) = self.call_api(PROD_URL)
        (stage_status, stage_json) = self.call_api(STAGING_URL)
        prod_json = ApiDiffer.normalise_absolute_urls(prod_json)
        stage_json = ApiDiffer.normalise_absolute_urls(stage_json)
        if prod_status != stage_status:
            lines = [
                f"* Received {prod_status} on prod and {stage_status} on stage",
                "",
                "prod:",
                f"{json.dumps(prod_json, indent=2)}",
                "",
                "stage:",
                f"{json.dumps(stage_json, indent=2)}",
            ]
            return ("different status", lines)
        elif prod_json == stage_json:
            return ("match", "")
        else:
            prod_pretty = json.dumps(prod_json, indent=2, sort_keys=True)
            stage_pretty = json.dumps(stage_json, indent=2, sort_keys=True)

            diff_lines = list(
                difflib.unified_diff(
                    prod_pretty.splitlines(),
                    stage_pretty.splitlines(),
                    fromfile="prod",
                    tofile="stage",
                )
            )

            if prod_json.keys() == stage_json.keys() and all(
                prod_json[k] == stage_json[k]
                for k in prod_json
                if k not in {"totalPages", "totalResults"}
            ):
                return ("different result count", diff_lines)
            else:
                return ("different JSON", diff_lines)

    def call_api(self, api_base):
        url = f"https://{api_base}{self.path}"
        response = httpx.get(url, params=self.params, follow_redirects=True)
        try:
            return (response.status_code, response.json())
        except json.JSONDecodeError:
            print(
                f"Non-JSON response received from {url}:\n---\n{response.text}\n---\n",
                file=sys.stderr,
            )
            sys.exit(1)


def _display_in_console(stats, diffs, outfile=None):
    def file_echo(*args, **kwargs):
        click.echo(*args, file=outfile, **kwargs)

    echo = file_echo if outfile else click.echo

    time_now = datetime.datetime.now().strftime("%A %-d %B %Y @ %H:%M:%S")
    echo()
    echo(click.style(f"API diff for {time_now}", fg="white", bold=True, underline=True))
    echo()
    echo(click.style("Index statistics", underline=True))
    echo()
    echo(
        tabulate(
            [
                ["Production"]
                + [humanize.intcomma(v) for v in stats["prod"]["work_types"].values()],
                ["Staging"]
                + [
                    humanize.intcomma(v)
                    for v in stats["staging"]["work_types"].values()
                ],
            ],
            headers=stats["prod"]["work_types"].keys(),
            colalign=("left", "right", "right", "right", "right", "right"),
        )
    )

    echo()
    echo(click.style("Index tests", underline=True))
    echo()

    for diff_line in diffs:
        if "comment" in diff_line["route"]:
            display_diff_line = diff_line["route"]["comment"]
        else:
            display_diff_line = diff_line["display_url"]

        if diff_line["status"] == "match":
            echo(click.style(f"✓ {display_diff_line}", fg="green"))
        elif diff_line["status"] == "different result count":
            echo(
                click.style(
                    f"! {display_diff_line} (result count differs)", fg="yellow"
                )
            )
        else:
            echo(click.style(f"✖ {display_diff_line}", fg="red"))

    echo()


@click.command()
@click.option(
    "--routes-file",
    default="routes.json",
    help="What routes file to use (default=routes.json)",
)
@click.option("--console", is_flag=True, help="Print results in console")
@click.option("--outfile", default=None)
def main(routes_file, console, outfile):
    with open(routes_file) as f:
        routes = json.load(f)

    def get_diff(route):
        differ = ApiDiffer(**route)
        status, diff_lines = differ.get_html_diff()

        return {
            "route": route,
            "display_url": differ.display_url,
            "status": status,
            "diff_lines": diff_lines,
        }

    with concurrent.futures.ThreadPoolExecutor() as executor:
        futures = [executor.submit(get_diff, r) for r in routes]
        concurrent.futures.wait(futures, return_when=concurrent.futures.ALL_COMPLETED)

        diffs = [fut.result() for fut in futures]

    stats = {
        label: api_stats.get_api_stats(api_url=api_url)
        for (label, api_url) in [("prod", PROD_URL), ("staging", STAGING_URL)]
    }

    if console:
        if outfile:
            with open(outfile, "w") as outfile_obj:
                _display_in_console(stats, diffs, outfile_obj)
        _display_in_console(stats, diffs)
    else:
        env = Environment(
            loader=FileSystemLoader("."), autoescape=select_autoescape(["html", "xml"])
        )

        env.filters["intcomma"] = humanize.intcomma

        template = env.get_template("template.html")
        html = template.render(now=datetime.datetime.now(), diffs=diffs, stats=stats)

        _, tmp_path = tempfile.mkstemp(suffix=".html")
        with open(tmp_path, "w") as outfile:
            outfile.write(html)

        os.system(f"open {tmp_path}")


if __name__ == "__main__":
    main()
