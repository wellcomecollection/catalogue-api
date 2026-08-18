#!/usr/bin/env python
"""
This script copies test documents from the pipeline repo into this repo.

For now, it's using a local clone, but eventually this script will be
extended to fetch from a remote repo on GitHub.
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

TARGET = "common/search/src/test/resources/test_documents"

# Mirror deletions too: a fixture removed upstream must not linger here as
# stale evidence for the OpenAPI response tests.
for stale in glob.glob(f"{TARGET}/*.json"):
    os.remove(stale)

for prefix in prefixes:
    for path in glob.glob(f"{PIPELINE_ROOT}/{prefix}/*.json"):
        shutil.copyfile(path, os.path.join(TARGET, os.path.basename(path)))
