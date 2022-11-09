#!/usr/bin/env bash
<<EOF
Finds the path to an sbt project directory.

This script is mirrored across our Scala repos.

== Motivation ==

In some of our repos, our projects are defined in the top level,
e.g. in catalogue-api

    .
    ├── items
    ├── requests
    └── search

but in other repos, there are sufficiently many sbt projects that we need
to nest them to keep the structure sensible. e.g. in storage-service

    .
    ├── bag_replicator
    ├── bag_verifier
    └── indexer/
        ├── bag_indexer
        ├── file_indexer
        └── ingests_indexer

Our build scripts need to know where a project is defined, so they can find
the Dockerfile and Docker Compose file.

sbt is the source of truth for this information, so this script asks sbt
to tell us the base directory for a given project.

== Usage examples ==

    $ get_sbt_project_directory.sh items
    items

    $ get_sbt_project_directory.sh file_indexer
    file_indexer

EOF

set -o errexit
set -o nounset

if (( $# == 1))
then
  PROJECT_NAME="$1"
else
  echo "Usage: run_sbt_tests.sh <PROJECT>" >&2
  exit 1
fi

ROOT=$(git rev-parse --show-toplevel)

# Note that the output from sbt ends with a carriage return (\r), which
# we need to discard.
BASE_DIR=$(
  ./builds/run_sbt_task_in_docker.sh -error "show $PROJECT_NAME" "stage; print baseDirectory" \
    | tr -d '\r'
)

echo $(relpath "$BASE_DIR" "$ROOT")
