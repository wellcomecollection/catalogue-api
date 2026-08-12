#!/usr/bin/env python
"""
This script copies test documents from a local clone of the pipeline repo.

The routine path is automated: the pipeline's sync-test-documents.yml
workflow opens a PR here when the documents change on its main branch.
Use this script for a local refresh from an unmerged pipeline branch.
"""

import glob
import os
import shutil
import sys


try:
    PIPELINE_ROOT = sys.argv[1]
except IndexError:
    sys.exit(f"Usage: {__file__} <PATH_TO_PIPELINE_REPO>")

prefixes = [
    "catalogue_graph/document_generators/test_documents",
]

for prefix in prefixes:
    for path in glob.glob(f"{PIPELINE_ROOT}/{prefix}/*.json"):
        shutil.copyfile(
            path,
            os.path.join(
                "common/search/src/test/resources/test_documents",
                os.path.basename(path),
            ),
        )
