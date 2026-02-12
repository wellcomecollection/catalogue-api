# Elasticsearch multi-cluster support

By default, the Search API connects to a single production Elasticsearch cluster (created automatically as part of our
Terraform stack and defined by its `pipelineDate`). The API also supports configuring additional clusters and routing
requests to them for experimental purposes.

## Architecture

Each additional cluster has a `ClusterConfig` object populated from `application.conf`. This object is used to create
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
