# Catalogue API

All the services that make up the Catalogue API

[![Build status](https://badge.buildkite.com/1d9006a0f151dd00522ff3ed59a247997016288b6b7ba30efa.svg?branch=main)](https://buildkite.com/wellcomecollection/catalogue-api)

## Overview

Contains the Catalogue APIs for search, snapshot, requesting and attendant ECS Script Tasks.

## Deploying

We deploy ECS catalogue services using the [weco-deploy](https://github.com/wellcomecollection/weco-deploy) tool.

The [current latest default branch](https://buildkite.com/wellcomecollection/catalogue-api) build deploys to staging automatically.

### Deploying to production

After automated deployment to the staging environment, we run [integration tests](https://buildkite.com/wellcomecollection/integration) against the staging API and front-end.

**When deploying a release from staging to production you should check these tests pass.**
