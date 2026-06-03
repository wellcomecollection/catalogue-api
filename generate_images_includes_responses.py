#!/usr/bin/env python3
"""
Generate expected response JSON files for ImagesIncludesTest from images.everything.json.

Reads the images.everything.json file and produces expected API response JSONs
under search/src/test/resources/expected_responses/include-{list|image}-{field}.json

These can then be used with `readResource("expected_responses/include-*.json")`
in the Scala test.
"""

import json
import os
from pathlib import Path

DOCS_DIR = Path("common/search/src/test/resources/test_documents")
OUTPUT_DIR = Path("search/src/test/resources/expected_responses")


def sort_json(obj):
    """Recursively sort JSON object keys alphabetically."""
    if isinstance(obj, dict):
        return {k: sort_json(v) for k, v in sorted(obj.items())}
    elif isinstance(obj, list):
        return [sort_json(item) for item in obj]
    return obj


def load_doc():
    """Load the images.everything document."""
    path = DOCS_DIR / "images.everything.json"
    with open(path) as f:
        return json.load(f)


def get_display(doc):
    """Get the display section of a document."""
    return doc["document"]["display"]


def minimal_image(display):
    """Return the image fields always present in responses."""
    return {
        "id": display["id"],
        "locations": display["locations"],
        "aspectRatio": display["aspectRatio"],
        "averageColor": display["averageColor"],
        "thumbnail": display["thumbnail"],
        "type": display["type"],
    }


def minimal_source(source):
    """Return the minimal source fields (always present)."""
    return {
        "id": source["id"],
        "title": source["title"],
        "type": source["type"],
    }


def image_with_include(display, include_field):
    """Return an image with the specified source include field."""
    image = minimal_image(display)
    source = minimal_source(display["source"])
    source[include_field] = display["source"].get(include_field, [])
    image["source"] = source
    return image


def make_list_response(display, include_field):
    """Create a list endpoint response with the given include."""
    return {
        "pageSize": 10,
        "results": [image_with_include(display, include_field)],
        "totalPages": 1,
        "totalResults": 1,
        "type": "ResultList",
    }


def make_single_response(display, include_field):
    """Create a single image endpoint response with the given include."""
    return image_with_include(display, include_field)


def write_json(path, obj):
    """Write sorted JSON with 2-space indentation."""
    sorted_obj = sort_json(obj)
    os.makedirs(path.parent, exist_ok=True)
    with open(path, "w") as f:
        json.dump(sorted_obj, f, indent=2)
        f.write("\n")
    print(f"  Written: {path}")


def main():
    doc = load_doc()
    display = get_display(doc)

    print(f"Image ID: {display['id']}")
    print(f"Source work ID: {display['source']['id']}")
    print()

    includes = [
        "contributors",
        "languages",
        "genres",
    ]

    for include in includes:
        # List endpoint
        list_response = make_list_response(display, include)
        write_json(
            OUTPUT_DIR / f"include-list-{include}.json", list_response
        )

        # Single image endpoint
        single_response = make_single_response(display, include)
        write_json(
            OUTPUT_DIR / f"include-image-{include}.json", single_response
        )

    print(f"\nDone! Generated {len(includes) * 2} files.")
    print(f"Image ID for tests: {display['id']}")


if __name__ == "__main__":
    main()
