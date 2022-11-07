#!/usr/bin/env bash
<<EOF
Finds the path to an sbt project directory.

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
    /Users/alexwlchan/repos/catalogue-api/items

    $ get_sbt_project_directory.sh file_indexer
    /Users/alexwlchan/repos/storage-service/indexer/file_indexer

EOF

set -o errexit
set -o nounset

PROJECT="$1"

ROOT=$(git rev-parse --show-toplevel)
BUILDS_DIR="$ROOT/builds"

# The "show project/baseDirectory" command will return output like
#
#     [info] welcome to sbt 1.4.1 (Homebrew Java 16.0.2)
#     [info] loading global plugins from /Users/alexwlchan/.sbt/1.0/plugins
#     [info] loading project definition from /Users/alexwlchan/repos/catalogue-api/project/project
#     [info] loading settings for project catalogue-api-build from build.sbt,plugins.sbt ...
#     [info] loading project definition from /Users/alexwlchan/repos/catalogue-api/project
#     [info] loading settings for project catalogue-api from build.sbt ...
#     [info] set current project to catalogue-api (in build file:/Users/alexwlchan/repos/catalogue-api/)
#     [info] Running in build environment: dev
#     [info] Installing the s3:// URLStreamHandler via java.net.URL.setURLStreamHandlerFactory
#     [info] /Users/alexwlchan/repos/catalogue-api/snapshots/snapshot_generator
#
# We want to grab the path from the final line.
#
$BUILDS_DIR/run_sbt_task_in_docker.sh "show $PROJECT/baseDirectory" \
  | tail -n 1 \
  | awk '{print $2}'
