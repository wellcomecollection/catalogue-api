import gzip
import json
import os
import re

from trim_snapshot import trim_snapshot


def get_latest_snapshot_filename():
    try:
        return max(
            [
                f
                for f in os.listdir()
                if re.match(r"works-\d{4}-\d{2}-\d{2}\.json\.gz", f)
            ]
        )
    except ValueError:  # max() arg is an empty sequence
        raise RuntimeError("No local snapshots found!")


def get_works(*, snapshot_filename=None, fields=None):
    """
    Generates a list of works from a given snapshot.
    If no snapshot is given, it uses the latest snapshot.

    To use this:

        from pprint import pprint
        from utils import get_works

        for work in get_works():
            pprint(work)
            break

    If you're only interested in a subset of fields, pass the ``fields``
    argument, e.g.:

        for work in get_works(fields=["items"]):
            pprint(work)
            break

    This will generate Works that only have that subset of fields populated.
    Limiting your analysis to a subset of fields can make this much faster
    after the initial run.

    """
    if snapshot_filename is None:
        snapshot_filename = get_latest_snapshot_filename()

    if fields is not None:
        snapshot_filename = trim_snapshot(
            snapshot_filename=snapshot_filename, fields=fields
        )

    for line in gzip.open(snapshot_filename):
        yield json.loads(line)


def get_items(snapshot_filename=None):
    for work in get_works(snapshot_filename):
        for item in work["items"]:
            yield work["id"], item


def get_locations(snapshot_filename=None):
    for work_id, item in get_items(snapshot_filename):
        for loc in item["locations"]:
            yield work_id, item.get("id"), loc


def get_source_identifier_str(work):
    source_identifier = work["identifiers"][0]

    return f"{source_identifier['identifierType']['id']}/{source_identifier['value']}"


def get_source_identifier_from_id(work_id):
    import httpx

    resp = httpx.get(
        f"https://api.wellcomecollection.org/catalogue/v2/works/{work_id}",
        params={"include": "identifiers"},
    )

    return get_source_identifier_str(resp.json())
