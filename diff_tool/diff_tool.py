#!/usr/bin/env python3

import collections.abc
import concurrent.futures
import datetime
import difflib
import json
import os
import sys
import tempfile
import re
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
    """Performs a diff against the same call to both sides of a comparison:
    by default the prod and stage works APIs, or, when an elastic cluster is
    given, the prod API with and without that cluster selected.
    """

    def __init__(self, path=None, params=None, elastic_cluster=None, **kwargs):
        self.path = f"/catalogue/v2{path}"
        self.params = params or {}
        self.elastic_cluster = elastic_cluster

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
    def side_labels(self):
        if self.elastic_cluster:
            return ("prod", self.elastic_cluster)
        return ("prod", "stage")

    @property
    def display_url(self):
        display_params = urllib.parse.urlencode(list(self.params.items()))
        if display_params:
            return f"{self.path}?{display_params}"
        else:
            return self.path

    @property
    def stage_display_url(self):
        """The comparison side's URL: in cluster mode, the prod path with the
        elasticCluster parameter included."""
        if not self.elastic_cluster:
            return self.display_url
        params = list(self.params.items()) + [("elasticCluster", self.elastic_cluster)]
        return f"{self.path}?{urllib.parse.urlencode(params)}"

    def get_html_diff(self):
        """
        Fetches a URL from the prod/staging API, and returns a (status, HTML diff).

        The possible statuses are:

            * different status = as in HTTP status
            * match = same JSON
            * different result count = everything is the same except totalResults/totalPages
            * different JSON = something is different

        """

        (prod_label, stage_label) = self.side_labels
        (prod_status, prod_json) = self.call_api(PROD_URL)
        if self.elastic_cluster:
            (stage_status, stage_json) = self.call_api(
                PROD_URL, extra_params={"elasticCluster": self.elastic_cluster}
            )
            stage_json = self.normalise_cluster_urls(stage_json)
        else:
            (stage_status, stage_json) = self.call_api(STAGING_URL)
        prod_json = ApiDiffer.normalise_absolute_urls(prod_json)
        stage_json = ApiDiffer.normalise_absolute_urls(stage_json)
        if prod_status != stage_status:
            lines = [
                f"* Received {prod_status} on {prod_label} and {stage_status} on {stage_label}",
                "",
                f"{prod_label}:",
                f"{json.dumps(prod_json, indent=2)}",
                "",
                f"{stage_label}:",
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
                    fromfile=prod_label,
                    tofile=stage_label,
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

    def normalise_cluster_urls(self, json):
        """
        Removes the elasticCluster parameter from URLs (eg pagination links) so
        that both sides of a cluster comparison take the same form.
        """

        def _normalise(data, remaining):
            for key, val in remaining.items():
                if isinstance(val, collections.abc.Mapping):
                    data[key] = _normalise(data.get(key, {}), val)
                elif isinstance(val, str):
                    data[key] = re.sub(
                        rf"[?&]elasticCluster={self.elastic_cluster}", "", val
                    )
                else:
                    data[key] = val
            return data

        return _normalise({}, json)

    def call_api(self, api_base, extra_params=None):
        url = f"https://{api_base}{self.path}"
        params = {**self.params, **(extra_params or {})}
        response = httpx.get(url, params=params, follow_redirects=True)
        try:
            return (response.status_code, response.json())
        except json.JSONDecodeError:
            print(
                f"Non-JSON response received from {url}:\n---\n{response.text}\n---\n",
                file=sys.stderr,
            )
            sys.exit(1)


def _display_in_console(stats, diffs, outfile=None, side_names=("Production", "Staging")):
    def file_echo(*args, **kwargs):
        click.echo(*args, file=outfile, **kwargs)

    echo = file_echo if outfile else click.echo

    time_now = datetime.datetime.now().strftime("%A %-d %B %Y @ %H:%M:%S")
    echo()
    echo(click.style(f"API diff for {time_now}", fg="white", bold=True, underline=True))
    echo()
    echo(click.style("Index statistics", underline=True))
    echo()
    # Align the comparison row to the production row's column order: the two
    # APIs do not return work types in a stable shared order.
    work_type_keys = list(stats["prod"]["work_types"].keys())
    echo(
        tabulate(
            [
                [side_names[0]]
                + [
                    humanize.intcomma(stats["prod"]["work_types"][k])
                    for k in work_type_keys
                ],
                [side_names[1]]
                + [
                    humanize.intcomma(stats["staging"]["work_types"].get(k, 0))
                    for k in work_type_keys
                ],
            ],
            headers=work_type_keys,
            colalign=("left", "right", "right", "right", "right", "right"),
        )
    )

    echo()
    echo(click.style("API tests", underline=True))
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
@click.option(
    "--elastic-cluster",
    default=None,
    help="Compare prod against prod with this elasticCluster selected "
    "(eg axiell-collections-testing), instead of against staging",
)
def main(routes_file, console, outfile, elastic_cluster):
    with open(routes_file) as f:
        routes = json.load(f)

    def get_diff(route):
        differ = ApiDiffer(**route, elastic_cluster=elastic_cluster)
        status, diff_lines = differ.get_html_diff()

        return {
            "route": route,
            "display_url": differ.display_url,
            "stage_display_url": differ.stage_display_url,
            "status": status,
            "diff_lines": diff_lines,
        }

    with concurrent.futures.ThreadPoolExecutor() as executor:
        futures = [executor.submit(get_diff, r) for r in routes]
        concurrent.futures.wait(futures, return_when=concurrent.futures.ALL_COMPLETED)

        diffs = [fut.result() for fut in futures]

    if elastic_cluster:
        stat_sources = [
            ("prod", PROD_URL, None),
            ("staging", PROD_URL, elastic_cluster),
        ]
    else:
        stat_sources = [("prod", PROD_URL, None), ("staging", STAGING_URL, None)]
    stats = {
        label: api_stats.get_api_stats(api_url=api_url, elastic_cluster=cluster)
        for (label, api_url, cluster) in stat_sources
    }

    side_names = ("Production", elastic_cluster or "Staging")

    if console:
        if outfile:
            with open(outfile, "w") as outfile_obj:
                _display_in_console(stats, diffs, outfile_obj, side_names)
        _display_in_console(stats, diffs, side_names=side_names)
    else:
        env = Environment(
            loader=FileSystemLoader("."), autoescape=select_autoescape(["html", "xml"])
        )

        env.filters["intcomma"] = humanize.intcomma

        template = env.get_template("template.html")
        html = template.render(
            now=datetime.datetime.now(),
            diffs=diffs,
            stats=stats,
            side_labels=("prod", elastic_cluster or "staging"),
            stage_api_base=PROD_URL if elastic_cluster else STAGING_URL,
        )

        _, tmp_path = tempfile.mkstemp(suffix=".html")
        with open(tmp_path, "w") as outfile:
            outfile.write(html)

        os.system(f"open {tmp_path}")


if __name__ == "__main__":
    main()
