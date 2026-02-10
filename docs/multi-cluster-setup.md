# Elasticsearch multi-cluster support

By default, the Search API connects to a single production Elasticsearch cluster (created automatically as part of our
Terraform stack and defined by its `pipelineDate`). The API also supports configuring additional clusters, allowing
you to:

- Connect to multiple Elasticsearch clusters simultaneously
- Route requests to different clusters
- Run experiments (A/B testing) with different indices

## Architecture

Each additional cluster has a `ClusterConfig` object populated from `application.conf`. This object is used to create
corresponding `ResilientElasticClient` and `WorksController` objects which are used when routing requests to the
cluster.

To route a request to a specific cluster, include a `elasticCluster` path parameter in the request containing the label
of the cluster:

```
/works?elasticCluster=someClusterLabel
```

If the `elasticCluster` parameter is missing, the request is routed to the default cluster.

## Configuring an additional cluster

To configure an additional cluster, add a `multiCluster` configuration block into the `application.conf` file. The
example configuration below adds a cluster labelled `someCluster`:

```hocon
multiCluster {
  someCluster {
    hostSecretPath = "some/secretsmanager/path"
    apiKeySecretPath = "some/secretsmanager/path"
    worksIndex = "works-experimental-v1"
    imagesIndex = "images-experimental-v1"
    portSecretPath = "some/secretsmanager/path"
    protocolSecretPath = "some/secretsmanager/path"
    semanticModelId = ".elser-2-elasticsearch"
    semanticVectorType = "sparse|dense"
  }
}
```
