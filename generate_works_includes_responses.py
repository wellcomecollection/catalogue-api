#!/usr/bin/env python3
"""
Generate expected response JSON files for WorksIncludesTest from test document sources.

Reads the work.visible.everything.{0,1,2}.json files and produces expected API
response JSONs under search/src/test/resources/expected_responses/works-include-*.json

These can then be used with `readResource("expected_responses/works-include-*.json")`
in the Scala test.
"""

import json
import os
from pathlib import Path

DOCS_DIR = Path("common/search/src/test/resources/test_documents")
OUTPUT_DIR = Path("search/src/test/resources/expected_responses")

# The API serialises JSON keys in alphabetical order
def sort_json(obj):
    """Recursively sort JSON object keys alphabetically."""
    if isinstance(obj, dict):
        return {k: sort_json(v) for k, v in sorted(obj.items())}
    elif isinstance(obj, list):
        return [sort_json(item) for item in obj]
    return obj


def load_docs():
    """Load all 3 work.visible.everything documents, sorted by ID."""
    docs = []
    for i in range(3):
        path = DOCS_DIR / f"work.visible.everything.{i}.json"
        with open(path) as f:
            docs.append(json.load(f))
    # Sort by document ID (the API returns results sorted by ID)
    docs.sort(key=lambda d: d["id"])
    return docs


def get_display(doc):
    """Get the display section of a document."""
    return doc["document"]["display"]


def minimal_work(display):
    """Return the minimal work fields (always present in responses)."""
    return {
        "id": display["id"],
        "title": display["title"],
        "alternativeTitles": display.get("alternativeTitles", []),
        "availabilities": display.get("availabilities", []),
        "type": display["type"],
    }


def strip_identifiers_from_items(items):
    """Strip identifiers from items (API doesn't include them without ?include=identifiers)."""
    result = []
    for item in items:
        item_copy = {k: v for k, v in item.items() if k != "identifiers"}
        result.append(item_copy)
    return result


def strip_identifiers_from_concepts(entries):
    """Strip identifiers from concepts within subjects/genres."""
    result = []
    for entry in entries:
        entry_copy = dict(entry)
        if "concepts" in entry_copy:
            entry_copy["concepts"] = [
                {k: v for k, v in concept.items() if k != "identifiers"}
                for concept in entry_copy["concepts"]
            ]
        result.append(entry_copy)
    return result


def work_with_include(display, include_field):
    """Return a work with minimal fields plus the specified include field."""
    work = minimal_work(display)
    value = display.get(include_field, [])
    # The API strips identifiers from nested objects unless ?include=identifiers is also passed
    if include_field == "items":
        value = strip_identifiers_from_items(value)
    elif include_field in ("subjects", "genres"):
        value = strip_identifiers_from_concepts(value)
    work[include_field] = value
    return work


def make_list_response(docs, include_field):
    """Create a list endpoint response with the given include."""
    results = []
    for doc in docs:
        display = get_display(doc)
        results.append(work_with_include(display, include_field))
    return {
        "pageSize": 10,
        "results": results,
        "totalPages": 1,
        "totalResults": len(results),
        "type": "ResultList",
    }


def make_single_response(doc, include_field):
    """Create a single work endpoint response with the given include."""
    display = get_display(doc)
    return work_with_include(display, include_field)


def write_json(path, obj):
    """Write sorted JSON with 2-space indentation."""
    sorted_obj = sort_json(obj)
    os.makedirs(path.parent, exist_ok=True)
    with open(path, "w") as f:
        json.dump(sorted_obj, f, indent=2)
        f.write("\n")
    print(f"  Written: {path}")


def main():
    docs = load_docs()
    # Use the first doc (by sorted ID) as the "single work" target
    single_doc = docs[0]
    single_id = single_doc["id"]

    print(f"Document IDs (sorted): {[d['id'] for d in docs]}")

    includes = [
        "identifiers",
        "items",
        "subjects",
        "genres",
        "contributors",
        "production",
        "languages",
        "notes",
        "formerFrequency",
        "designation",
        "images",
        "parts",
        "partOf",
        "precededBy",
        "succeededBy",
        "holdings",
    ]

    for include in includes:
        # List endpoint
        list_response = make_list_response(docs, include)
        write_json(
            OUTPUT_DIR / f"works-include-list-{include}.json", list_response
        )

        # Single work endpoint
        single_response = make_single_response(single_doc, include)
        write_json(
            OUTPUT_DIR / f"works-include-single-{include}.json", single_response
        )

    print(f"\nDone! Generated {len(includes) * 2} files.")
    print(f"Single work ID for tests: {single_id}")


if __name__ == "__main__":
    main()
