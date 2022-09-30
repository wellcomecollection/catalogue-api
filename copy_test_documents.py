#!/usr/bin/env python
"""
This script copies test documents from the pipeline repo into this repo.

For now, it's using a local clone, but eventually this script will be
extended to fetch from a remote repo on GitHub.
"""

import glob
import os
import shutil


PIPELINE_ROOT = os.path.join(os.environ["HOME"], "repos/pipeline")

prefixes = [
    "common/internal_model/src/test/resources",
    "pipeline/ingestor/test_documents",
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

os.rename(
    "./common/search/src/test/resources/test_documents/WorksIndexConfig.json",
    "./common/search/src/test/resources/WorksIndexConfig.json",
)
os.rename(
    "./common/search/src/test/resources/test_documents/ImagesIndexConfig.json",
    "./common/search/src/test/resources/ImagesIndexConfig.json",
)
