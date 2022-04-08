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

            return (
                "different JSON",
                list(
                    difflib.unified_diff(
                        prod_pretty.splitlines(),
                        stage_pretty.splitlines(),
                        fromfile="prod",
                        tofile="stage",
                    )
                ),
            )

    def call_api(self, api_base):
        url = f"https://{api_base}{self.path}"
        response = httpx.get(url, params=self.params)
        try:
            return (response.status_code, response.json())
        except json.JSONDecodeError:
            print(f"Non-JSON response received from {url}:\n---\n{response.text}\n---\n", file=sys.stderr)
            sys.exit(1)


def _display_in_console(stats, diffs):
    time_now = datetime.datetime.now().strftime("%A %-d %B %Y @ %H:%M:%S")
    click.echo()
    click.echo(
        click.style(f"API diff for {time_now}", fg="white", bold=True, underline=True)
    )
    click.echo()
    click.echo(click.style("Index statistics", underline=True))
    click.echo()
    click.echo(
        tabulate(
            [
                ["Production"] + list(stats["prod"]["work_types"].values()),
                ["Staging"] + list(stats["staging"]["work_types"].values()),
            ],
            headers=stats["prod"]["work_types"].keys(),
        )
    )

    click.echo()
    click.echo(click.style("Index tests", underline=True))
    click.echo()

    for diff_line in diffs:
        if "comment" in diff_line["route"]:
            display_diff_line = diff_line["route"]["comment"]
        else:
            display_diff_line = diff_line["display_url"]

        if diff_line["status"] == "match":
            click.echo(click.style(f"✓ {display_diff_line}", fg="green"))
        else:
            click.echo(click.style(f"✖ {display_diff_line}", fg="red"))

    click.echo()


@click.command()
@click.option(
    "--routes-file",
    default="routes.json",
    help="What routes file to use (default=routes.json)",
)
@click.option("--console", is_flag=True, help="Print results in console")
def main(routes_file, console):
    session = api_stats.get_session_with_role(
        role_arn="arn:aws:iam::756629837203:role/catalogue-ci"
    )

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
        label: api_stats.get_api_stats(session, api_url=api_url)
        for (label, api_url) in [("prod", PROD_URL), ("staging", STAGING_URL)]
    }

    if console:
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
