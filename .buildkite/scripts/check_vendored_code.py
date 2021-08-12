#!/usr/bin/env python3
"""
We copy a small number of files out of the catalogue-pipeline repo and
into the catalogue-api repo.  These are rules around Sierra item data,
which we use in both the Sierra transformer and the items API.

Why not publish them in a library across the repos?  Because it's only
a small number of files, and copy/pasting creates less gnarly dependency
problems them doing a full library.  In particular, the versions in the
pipeline repo are closely tied to that version of internal_model -- copying
the files across means we can step these rules without stepping internal_model.

It might be nice to have some sort of auto-pull request mechanism, a la
scala-libs, but for now this is deliberately light-touch.

I don't expect these rules to change much, so this script is more about
reminding us to stay up-to-date than doing it for us.  We can add more
automation if it seems useful.

"""

import os
import subprocess
import sys
import urllib.request


PATHS_TO_VENDOR = {
    "src/main/scala/weco/catalogue/source_model/sierra/rules/RulesForRequestingResult.scala",
    "src/main/scala/weco/catalogue/source_model/sierra/rules/SierraItemAccess.scala",
    "src/main/scala/weco/catalogue/source_model/sierra/rules/SierraPhysicalLocationType.scala",
    "src/main/scala/weco/catalogue/source_model/sierra/rules/SierraRulesForRequesting.scala",
    "src/main/scala/weco/catalogue/source_model/sierra/source/OpacMsg.scala",
    "src/main/scala/weco/catalogue/source_model/sierra/source/Status.scala",
    "src/test/scala/weco/catalogue/source_model/sierra/rules/SierraItemAccessTest.scala",
    "src/test/scala/weco/catalogue/source_model/sierra/rules/SierraPhysicalLocationTypeTest.scala",
    "src/test/scala/weco/catalogue/source_model/sierra/rules/SierraRulesForRequestingTest.scala",
}

SRC_PREFIX = "common/source_model"
DST_PREFIX = "common/stacks"

GITHUB_SRC_REPO_PREFIX = "https://raw.githubusercontent.com/wellcomecollection/catalogue-pipeline/main"


if __name__ == '__main__':
    for p in PATHS_TO_VENDOR:
        url = os.path.join(GITHUB_SRC_REPO_PREFIX, SRC_PREFIX, p)
        dst_path = os.path.join(DST_PREFIX, p)
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        urllib.request.urlretrieve(url, dst_path)

    try:
        subprocess.check_call(["git", "diff", "--exit-code"])
    except subprocess.CalledProcessError:
        sys.exit(
            "The source_model files in this repo don't match the files in the catalogue-pipeline repo. Please update them!"
        )
