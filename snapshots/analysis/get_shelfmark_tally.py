#!/usr/bin/env python
"""
Creates a spreadsheet showing all our shelfmarks, a tally, and an example
of each one.
"""

import collections
import csv
import sys

from utils import get_locations


if __name__ == "__main__":
    try:
        filename = sys.argv[1]
    except IndexError:
        sys.exit(f"Usage: {__file__} <FILENAME>")

    tally = collections.defaultdict(set)

    for work_id, _, loc in get_locations(filename):
        tally[loc.get("shelfmark")].add(work_id)

    with open("shelfmark_tally.csv", "w") as outfile:
        writer = csv.DictWriter(
            outfile, fieldnames=["shelfmark", "count", "example_work_id"]
        )
        writer.writeheader()

        for shelfmark, ids in sorted(
            tally.items(), key=lambda kv: len(kv[1]), reverse=True
        ):
            writer.writerow(
                {
                    "shelfmark": shelfmark,
                    "count": len(ids),
                    "example_work_id": sorted(ids)[0],
                }
            )

    print("shelfmark_tally.csv")
