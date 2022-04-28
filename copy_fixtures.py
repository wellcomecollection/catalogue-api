#!/usr/bin/env python

import glob
import os
import shutil


PIPELINE_ROOT = os.path.join(os.environ["HOME"], "repos/pipeline")

prefixes = [
    "common/internal_model/src/test/resources",
    "fixtures",
]

for prefix in prefixes:
    for path in glob.glob(f"{PIPELINE_ROOT}/{prefix}/*.json"):
        shutil.copyfile(
            path,
            os.path.join(
                "common/search/src/test/resources/fixtures", os.path.basename(path)
            ),
        )
