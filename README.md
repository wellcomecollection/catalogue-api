# Catalogue API

All the services that make up the Catalogue API

[![Build status](https://badge.buildkite.com/1d9006a0f151dd00522ff3ed59a247997016288b6b7ba30efa.svg?branch=main)](https://buildkite.com/wellcomecollection/catalogue-api)

## Overview

Contains the Catalogue APIs for search, snapshot, requesting and attendant ECS Script Tasks.

## Deployment

Deploying the search API:

### Steps

* open [`terraform/locals.tf`](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/api/terraform/locals.tf)
* there are two versions of the API deployed, `romulus` and `remus`
* `api.wellcomecollection.org` will be pointing to the deployment referenced in `production_api`. `stage-api.` will

  be pointing to the other.

* change the ECS container reference to your new version on the staging deployment
* `terraform apply`
* test on `api-stage.`
* once satisfied, change `production_api` to the new deployment

