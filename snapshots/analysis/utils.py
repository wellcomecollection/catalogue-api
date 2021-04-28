import gzip
import json
import os
import re


def get_latest_snapshot_filename():
    try:
        return max([
            f
            for f in os.listdir()
            if re.match(r"works-\d{4}-\d{2}-\d{2}\.json\.gz", f)
        ])
    except ValueError:  # max() arg is an empty sequence
        raise RuntimeError("No local snapshots found!")


def get_works(snapshot_filename=None):
    """
    Generates a list of works from a given snapshot.
    If no snapshot is given, it uses the latest snapshot.

    To use this:

        from pprint import pprint
        from utils import get_works

        for work in get_works():
            pprint(work)
            break

    """
    if snapshot_filename is None:
        snapshot_filename = _get_latest_snapshot_filename()

    for line in gzip.open(snapshot_filename):
        yield json.loads(line)


