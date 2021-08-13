#!/usr/bin/env python
"""
As part of the work to put item access status on wc.org/works, there
are some items whose status we couldn't map correctly.

In these cases, we display a placeholder message:

    This item cannot be requested online. Please contact
    library@wellcomecollection.org for more information.

This script analyses a catalogue API snapshot and produces a spreadsheet
showing all the items with unmapped data.

This can be passed to the Collections team for sorting out in Sierra proper.

"""

import csv
import functools
import json

import boto3
import tqdm

from utils import get_works


PLACEHOLDER_MESSAGE = (
    "This item cannot be requested online. "
    'Please contact <a href="mailto:library@wellcomecollection.org">library@wellcomecollection.org</a> for more information.'
)

SIERRA_ADAPTER_TABLE_NAME = "vhs-sierra-sierra-adapter-20200604"


def find_sierra_id(identifiers):
    return next(
        id for id in identifiers if id["identifierType"]["id"] == "sierra-system-number"
    )["value"]


def find_unmapped_item_ids():
    """
    Generates (bib id, item id) pairs for items that haven't been mapped
    correctly.
    """
    for work in get_works(fields=["items"]):
        for item in work["items"]:
            locations = item["locations"]

            if not any(loc["type"] == "PhysicalLocation" for loc in locations):
                continue

            for loc in locations:
                if any(
                    ac.get("note") == PLACEHOLDER_MESSAGE
                    for ac in loc["accessConditions"]
                ):
                    bib_id = find_sierra_id(work["identifiers"])
                    item_id = find_sierra_id(item["identifiers"])

                    yield (bib_id, item_id)


@functools.lru_cache()
def get_sierra_data(bib_id):
    dynamo = boto3.resource("dynamodb").meta.client
    s3 = boto3.client("s3")

    dynamo_item = dynamo.get_item(
        TableName=SIERRA_ADAPTER_TABLE_NAME, Key={"id": bib_id[1:8]}
    )
    s3_location = dynamo_item["Item"]["payload"]

    s3_object = json.load(
        s3.get_object(Bucket=s3_location["bucket"], Key=s3_location["key"])["Body"]
    )

    return s3_object


def get_bib_506(sierra_data):
    """
    bib field 506 is used for the access status on item records.
    """
    bib_data = json.loads(sierra_data["maybeBibRecord"]["data"])

    varfields_506 = [vf for vf in bib_data["varFields"] if vf.get("marcTag") == "506"]

    if not varfields_506:
        return ""

    lines = []

    for vf in varfields_506:
        subfields = " ".join(f"{sf['tag']}|{sf['content']}" for sf in vf["subfields"])

        if subfields.startswith("a|"):
            lines.append(subfields[len("a|") :])
        else:
            lines.append(subfields)

    return "\n".join(lines)


def get_fixed_field(item_data, *, code):
    try:
        ff = item_data["fixedFields"][code]
    except KeyError:
        return ""
    else:
        try:
            return f"{ff['value']} ({ff['display']})"
        except KeyError:
            return ff["value"]


if __name__ == "__main__":
    from pprint import pprint

    with open("unmapped_item_data.csv", "w") as outfile:
        writer = csv.DictWriter(
            outfile,
            fieldnames=[
                "bib number",
                "item number",
                "hold count",
                "bib 506 (restrictions on access note)",
                "item 79 (LOCATION)",
                "item 88 (STATUS)",
                "item 108 (OPACMSG)",
                "item 61 (I TYPE)",
                "item 97 (IMESSAGE)",
                "item 87 (LOANRULE)",
            ],
        )
        writer.writeheader()

        for bib_id, item_id in tqdm.tqdm(find_unmapped_item_ids()):
            sierra_data = get_sierra_data(bib_id)

            bib_506 = get_bib_506(sierra_data)

            item_data = json.loads(sierra_data["itemRecords"][item_id[1:8]]["data"])

            hold_count = item_data.get("holdCount", "")

            writer.writerow(
                {
                    "bib number": bib_id,
                    "item number": item_id,
                    "hold count": hold_count,
                    "bib 506 (restrictions on access note)": bib_506,
                    "item 61 (I TYPE)": get_fixed_field(item_data, code="61"),
                    "item 79 (LOCATION)": get_fixed_field(item_data, code="79"),
                    "item 87 (LOANRULE)": get_fixed_field(item_data, code="87"),
                    "item 88 (STATUS)": get_fixed_field(item_data, code="88"),
                    "item 97 (IMESSAGE)": get_fixed_field(item_data, code="97"),
                    "item 108 (OPACMSG)": get_fixed_field(item_data, code="108"),
                }
            )
