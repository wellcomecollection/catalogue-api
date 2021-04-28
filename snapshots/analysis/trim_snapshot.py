#!/usr/bin/env python3
"""
Create a snapshot which only contains certain, specified fields.

This is useful if you're only interested in a subset of fields -- it makes
a new snapshot file which can be substantially smaller, so you can page
through it much faster.

For a brief comparison, when I first wrote this script:

    complete snapshot = 1.4GB, 1m52s to page through
    just items/holdings = 73.1MB, 16s to page through

"""

import gzip
import json
import os
import uuid
import sys

import click
import tqdm

from utils import get_latest_snapshot_filename, get_works


def trim_snapshot(*, snapshot_filename, fields):
    fields = set(fields)
    fields.add("id")
    fields.add("identifiers")

    name, ext = snapshot_filename.split(".", 1)
    new_name = name + "_" + ",".join(sorted(fields)) + "." + ext

    if not os.path.exists(new_name):
        tmp_path = new_name + "." + str(uuid.uuid4()) + ".tmp"

        with gzip.open(tmp_path, "wb") as outfile:
            for work in tqdm.tqdm(get_works(snapshot_filename)):
                trimmed_work = {
                    key: value for key, value in work.items() if key in fields
                }

                # Compact JSON representation
                # See https://twitter.com/raymondh/status/842777864193769472
                line = json.dumps(trimmed_work, separators=(",", ":")) + "\n"

                outfile.write(line.encode("utf8"))

        os.rename(tmp_path, new_name)

    return new_name


def _size(filename):
    return os.stat(filename).st_size


@click.command()
@click.option("--filename", help="Snapshot to trim")
@click.argument("fields", nargs=-1)
def main(filename, fields):
    if filename is None:
        filename = get_latest_snapshot_filename()

    original_snapshot = filename
    trimmed_snapshot = trim_snapshot(snapshot_filename=filename, fields=fields)

    saving = abs(1 - _size(trimmed_snapshot) / _size(original_snapshot))

    print(
        "Trimming complete; the trimmed snapshot is %.1f%% smaller" % (saving * 100),
        file=sys.stderr,
    )
    print(trimmed_snapshot)


if __name__ == "__main__":
    main()
