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
- `semantic.vectorType` (if provided) must be either `sparse` or `dense`.

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
    semantic {
      modelId = ".elser-2-elasticsearch"
      vectorType = "sparse"
    }
  }
}
```

## The `new-pipeline` cluster

The `new-pipeline` entry points at the output of the new (Axiell/FOLIO) pipeline, so that its works and images
indexes can be previewed with `?elasticCluster=new-pipeline` before the default cluster is flipped over.

Like any other additional cluster, if its config fails to parse or its client fails to build at startup (e.g.
because a secret doesn't exist), the cluster is logged and dropped, and requests selecting it return 404.

Note for local development: the entry uses the cluster's `private_host` secret, which is only reachable from
inside the VPC. When running the API outside the VPC (e.g. locally), swap `hostSecretPath` to the corresponding
`public_host` secret (`elasticsearch/es_cluster_2026-07-03/public_host`).

### Eventually flipping the default

The environment default stays on the old pipeline until we're ready to cut over. To flip the default:

1. Update `defaultPipelineDate`, `defaultWorksIndexDate` and `defaultImagesIndexDate` in
   `common/search/src/main/scala/weco/api/search/models/ElasticConfig.scala`.
2. Deploy to stage, verify, then deploy to prod.

To roll back, revert the `ElasticConfig` change and redeploy. To remove the `new-pipeline` cluster entirely,
delete its `multiCluster` entry from `application.conf` and redeploy; requests selecting it will then return 404.
