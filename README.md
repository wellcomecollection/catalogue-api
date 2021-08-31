# Catalogue API

All the services that make up the Catalogue API

[![Build status](https://badge.buildkite.com/1d9006a0f151dd00522ff3ed59a247997016288b6b7ba30efa.svg?branch=main)](https://buildkite.com/wellcomecollection/catalogue-api)
[![Deploy stage](https://img.shields.io/buildkite/41057eb74a0e2c22d2f78c325bfa6b90458b9529b2bb532b85/main.svg?label=deploy%20stage)](https://buildkite.com/wellcomecollection/catalogue-api-deploy-stage)
[![Deploy prod](https://img.shields.io/buildkite/b7212f6ddcd97f0888e7e7c9064648100a66b54df7e00d5f97/main.svg?label=deploy%20prod)](https://buildkite.com/wellcomecollection/catalogue-api-deploy-prod)

## Overview

Contains the Catalogue APIs for search, snapshot, requesting and attendant ECS Script Tasks.

## Deploying

We deploy ECS catalogue services using the [weco-deploy](https://github.com/wellcomecollection/weco-deploy) tool.

### Continuous integration

The [current latest default branch](https://buildkite.com/wellcomecollection/catalogue-api) build [deploys to staging automatically](https://buildkite.com/wellcomecollection/catalogue-api-deploy-stage). 

After a deployment to stage environment, we run [smoke tests](smoke_tests/README.md) against the stage API and then [e2e tests](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/playwright/README.md) on the front-end pointing the production wellcomecollection.org at the stage catalogue API.

After a successful stage deployment we run the [diff_tool](diff_tool/README.md) and wait for a user to review and [deploy to production](https://buildkite.com/wellcomecollection/catalogue-api-deploy-prod).

We then run the same smoke & e2e tests pointed at production to confirm a successful deployment.

The CI flow looks as follows:

![Buildkite pipelines](buildkite_flow.png)

## Dependencies

* Java 1.8
* Scala 2.12
* SBT
* Terraform
* Docker
* Make
