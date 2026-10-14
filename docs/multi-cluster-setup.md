# Elasticsearch multi-cluster support

By default, the Search API connects to a single production Elasticsearch cluster (created automatically as part of our
Terraform stack and defined by its `pipelineDate`). The API also supports configuring additional clusters and routing
requests to them for experimental purposes.

## Architecture

Each additional cluster has an `ElasticConfig` object populated from `application.conf`. This object is used to create
corresponding `ResilientElasticClient` and `WorksController` objects which are used when routing requests to the
cluster.

To route a request to a specific cluster, include a `elasticCluster` query parameter in the request containing the label
of the cluster:

```
/works?elasticCluster=someClusterLabel
```

If the `elasticCluster` parameter is missing, the request is routed to the default cluster.

## Configuring an additional cluster

To configure an additional cluster, add a `multiCluster` configuration block into the `application.conf` file.

Only `hostSecretPath` and `apiKeySecretPath` are required. All other fields are optional:

- If `portSecretPath` and/or `protocolSecretPath` are omitted, the default cluster’s port/protocol are used.
- If `worksIndex` and/or `imagesIndex` are omitted, requests routed to those indexes return status 404.

The example configuration below adds a cluster labelled `someCluster`:

```hocon
multiCluster {
  someCluster {
    hostSecretPath = "some/secretsmanager/path"
    apiKeySecretPath = "some/secretsmanager/path"
    worksIndex = "works-experimental-v1"
    imagesIndex = "images-experimental-v1"
    portSecretPath = "some/secretsmanager/path"
    protocolSecretPath = "some/secretsmanager/path"
  }
}
```

## Previewing a new pipeline before flipping the default

A pipeline that is about to replace the default can be exposed as an additional cluster so that its works and
images indexes can be previewed with `?elasticCluster=<name>` before the flip. That was done with
`axiell-collections-testing` for the `2026-07-03` pipeline (the Axiell Collections switchover) and with
`pipeline-2026-09-30` for the `2026-09-30` pipeline (wellcomecollection/platform#6725). Each entry was removed once
its pipeline became the default. To do it again, add a `multiCluster.<name>` block whose
secret paths use the pipeline's `elasticsearch/pipeline_storage_<date>/` prefix (created by the `pipeline_new`
stack in the catalogue-pipeline repo) and whose `worksIndex` and `imagesIndex` name that pipeline's indexes.

Like any other additional cluster, if its config fails to parse or its client fails to build at startup (e.g.
because a secret doesn't exist), the cluster is logged and dropped, and requests selecting it return 404.

Note for local development: use the cluster's `public_host` secret rather than `private_host`, which is only
reachable from inside the VPC.

### Flipping the default

1. Update `defaultPipelineDate`, `defaultWorksIndexDate` and `defaultImagesIndexDate` in
   `common/search/src/main/scala/weco/api/search/models/ElasticConfig.scala`. These are shared by the
   search API, the items API and the snapshot generator.
2. Update `pipelineDate` and `conceptsIndex` in `concepts/config.ts`. The concepts API has its own
   config rather than the shared defaults.
3. Deploy to stage, verify, then deploy to prod.

To roll back, revert both changes and redeploy. Remove the preview entry from `application.conf` once the flip
has settled; requests selecting it will then return 404.
