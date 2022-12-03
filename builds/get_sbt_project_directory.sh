#!/usr/bin/env bash
<<EOF
Finds the path to an sbt project directory.

This script is mirrored across our Scala projects.

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

PROJECT="$1"

ROOT=$(git rev-parse --show-toplevel)
BUILDS_DIR="$ROOT/builds"

# https://stackoverflow.com/a/31236568/1558022
function relpath() {
  python3 -c "import os, sys; print(os.path.relpath(*(sys.argv[1:])))" "$@";
}

BASE_DIR=$(
  $BUILDS_DIR/run_sbt_task_in_docker.sh -error "project $PROJECT" "print baseDirectory" \
    | tr -d '\n'
)

echo $(relpath "$BASE_DIR" "$ROOT")
