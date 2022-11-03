#!/usr/bin/env python3

import os

from commands import sbt


# This script takes environment variables as the "command" step
# when used with the buildkite docker plugin incorrectly parses
# spaces as newlines preventing passing args to this script!
if __name__ == "__main__":
    project = os.environ.get("PROJECT")
    sbt(f"project {project}", "publish")
