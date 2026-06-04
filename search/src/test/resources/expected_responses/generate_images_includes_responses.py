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
import subprocess
from pathlib import Path
from typing import Any

ROOT = Path(subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip())
DOCS_DIR = ROOT / "common/search/src/test/resources/test_documents"
OUTPUT_DIR = ROOT / "search/src/test/resources/expected_responses"

MINIMAL_IMAGE_FIELDS = ["id", "locations", "aspectRatio", "averageColor", "thumbnail", "type"]
MINIMAL_SOURCE_WORK_FIELDS = ["id", "title", "type"]


def load_doc() -> dict[str, Any]:
    """Load the images.everything document."""
    path = DOCS_DIR / "images.everything.json"
    with open(path) as f:
        return json.load(f)


def minimal_image(display: dict[str, Any]) -> dict[str, Any]:
    """Return the image fields always present in responses."""
    return {field: display[field] for field in MINIMAL_IMAGE_FIELDS}


def minimal_source(source: dict[str, Any]) -> dict[str, Any]:
    """Return the minimal source fields (always present)."""
    return {field: source[field] for field in MINIMAL_SOURCE_WORK_FIELDS}


def image_with_include(display: dict[str, Any], include_field: str) -> dict[str, Any]:
    """Return an image with the specified source include field."""
    image = minimal_image(display)
    source = minimal_source(display["source"])
    source[include_field] = display["source"].get(include_field, [])
    image["source"] = source
    return image


def make_list_response(display: dict[str, Any], include_field: str) -> dict[str, Any]:
    """Create a list endpoint response with the given include."""
    return {
        "pageSize": 10,
        "results": [image_with_include(display, include_field)],
        "totalPages": 1,
        "totalResults": 1,
        "type": "ResultList",
    }


def make_single_response(display: dict[str, Any], include_field: str) -> dict[str, Any]:
    """Create a single image endpoint response with the given include."""
    return image_with_include(display, include_field)


def write_json(path: Path, obj: dict[str, Any]) -> None:
    """Write sorted JSON with 2-space indentation."""
    os.makedirs(path.parent, exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2, sort_keys=True)
        f.write("\n")
    print(f"  Written: {path}")


def main() -> None:
    doc = load_doc()
    display = doc["document"]["display"]

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
        write_json(OUTPUT_DIR / f"include-list-{include}.json", list_response)

        # Single image endpoint
        single_response = make_single_response(display, include)
        write_json(OUTPUT_DIR / f"include-image-{include}.json", single_response)

    print(f"\nDone! Generated {len(includes) * 2} files.")
    print(f"Image ID for tests: {display['id']}")


if __name__ == "__main__":
    main()
