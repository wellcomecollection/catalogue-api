#!/usr/bin/env python
"""
This script goes through a catalogue snapshot and finds examples of
every character in use.

It produces a spreadsheet like:

    character,Unicode name,Unicode category,example work
    D,LATIN CAPITAL LETTER D,Lu,avx2b5c4
    6,DIGIT SIX,Nd,avx2b5c4
    d,LATIN SMALL LETTER D,Ll,avx2b5c4
    G,LATIN CAPITAL LETTER G,Lu,avx2b5c4

We used this in April 2022 when we were deciding whether to switch the
font on the website; this script let us look at examples of works with
every character to check they were supported by the candidate font.

"""

import csv
from pprint import pprint
import sys
import unicodedata

from utils import get_works


def get_used_characters(json_object):
    if isinstance(json_object, (str, int)):
        return set(str(json_object))
    elif isinstance(json_object, dict):
        return get_used_characters(list(json_object.values()))
    elif isinstance(json_object, list):
        result = set()
        for list_item in json_object:
            result.update(get_used_characters(list_item))
        return result
    else:
        raise ValueError(f"Unrecognised type: {type(json_object)}")


if __name__ == "__main__":
    seen_characters = set()

    with open("character_analysis.csv", "w") as outfile:
        writer = csv.DictWriter(
            outfile,
            fieldnames=[
                "character",
                "Unicode name",
                "Unicode category",
                "example work",
            ],
        )
        writer.writeheader()

        for w in get_works():
            work_id = w["id"]
            characters = get_used_characters(w)

            for char in characters:
                if char not in seen_characters:
                    writer.writerow(
                        {
                            "character": char,
                            "Unicode name": unicodedata.name(char),
                            "Unicode category": unicodedata.category(char),
                            "example work": work_id,
                        }
                    )
