# Multi-Cluster Elasticsearch Support

This document describes how to configure the Search API to read from multiple Elasticsearch clusters.

## Overview

The multi-cluster support allows you to:

- Connect to multiple Elasticsearch clusters simultaneously
- Route different API endpoints to different clusters
- Support both standard pipeline clusters and custom/serverless clusters
- Run experiments (A/B testing) with different indices
- Gradually migrate to new cluster architectures

## Architecture

### Components

1. **MultiClusterElasticConfig** - Configuration model supporting multiple named clusters
2. **ClusterConfig** - Configuration for a single cluster (pipeline or custom)
3. **MultiClusterElasticClientBuilder** - Builds ES clients for different cluster types
4. **MultiClusterSearchApi** - Router that directs requests to appropriate clusters
5. **MultiClusterConfigParser** - Parses configuration from application.conf
6. **MultiClusterMain** - Alternative main class with multi-cluster support

### Cluster Types

#### 1. Pipeline Cluster (Standard)

Uses the existing pipeline storage pattern with AWS Secrets Manager:

- Hostname: `elasticsearch/pipeline_storage_<date>/private_host`
- API Key: `elasticsearch/pipeline_storage_<date>/catalogue_api/api_key`
- Port: `elasticsearch/pipeline_storage_<date>/port`

#### 2. Custom Cluster (Serverless, etc.)

Direct connection with custom credentials:

- Hostname: Configured directly (e.g., `my-project.es.us-east-1.aws.elastic.cloud`)
- API Key: From custom Secrets Manager path
- Port: Configurable (default 9243)
- Protocol: Configurable (default https)

## Configuration

### Option 1: Environment Variables

```bash
# Serverless cluster example
export multiCluster__xp_a__customHost="my-serverless.es.aws.elastic.cloud"
export multiCluster__xp_a__customApiKeySecretPath="elasticsearch/xp-a/api_key"
export multiCluster__xp_a__worksIndex="works-experimental-v1"

# Another pipeline cluster
export multiCluster__xp_b__pipelineDate="2025-12-01"
export multiCluster__xp_b__worksIndex="works-indexed-2025-12-01"
```

Note: Use double underscores for nested properties and replace hyphens with underscores in names.

### Option 2: application.conf

```hocon
multiCluster {
  xp-a {
    # For serverless ES
    customHost = "my-serverless-project.es.us-east-1.aws.elastic.cloud"
    customPort = 443
    customProtocol = "https"
    customApiKeySecretPath = "elasticsearch/xp-a/catalogue_api/api_key"
    worksIndex = "works-experimental-v1"
    imagesIndex = "images-experimental-v1"
  }

  xp-b {
    # For another pipeline cluster
    pipelineDate = "2025-12-01"
    worksIndex = "works-indexed-2025-12-01"
    imagesIndex = "images-indexed-2025-12-01"
  }
}
```

#### Default Cluster Routes (unchanged)

- `GET /works` - List/search works
- `GET /works/{id}` - Get single work
- `GET /images` - List/search images
- `GET /images/{id}` - Get single image

#### Experimental Cluster Routes

- `GET /works/xp-a` - List/search works from xp-a cluster
- `GET /works/xp-a/{id}` - Get single work from xp-a cluster
- `GET /works/xp-b` - List/search works from xp-b cluster
- `GET /works/xp-b/{id}` - Get single work from xp-b cluster

### 5. Configure Application

```hocon
multiCluster {
  xp-a {
    customHost = "catalogue-xp-a-abc123.us-east-1.es.aws.amazon.com"
    customPort = 443
    customProtocol = "https"
    customApiKeySecretPath = "elasticsearch/xp-a/api_key"
    worksIndex = "works-xp-a"
  }
}
```

## Adding More Experimental Routes

To add routes for additional cluster types (e.g., `/works/xp-b/`, `/images/xp-a/`), edit the `MultiClusterSearchApi.scala`:

```scala
def routes: Route = {
  concat(
    // Cluster A works
    pathPrefix("works" / "xp-a") {
      // ... existing code ...
    },

    // Cluster B works
    pathPrefix("works" / "xp-b") {
      getWorksControllerForCluster("xp-b") match {
        case Some(controller) => // ... route implementation
        case None => notFound("Cluster 'xp-b' is not configured")
      }
    },

    // Cluster A images
    pathPrefix("images" / "xp-a") {
      getImagesControllerForCluster("xp-a") match {
        case Some(controller) => // ... route implementation
        case None => notFound("Cluster 'xp-a' is not configured")
      }
    },

    // Default routes
    defaultSearchApi.routes
  )
}
```

## Backward Compatibility

The system is fully backward compatible:

- If no `multiCluster` configuration is present, only the default cluster is used
- Existing routes continue to work unchanged
- You can use `MultiClusterMain` even without multi-cluster config
- You can continue using the original `Main` class if you don't need multi-cluster support

## Monitoring and Debugging

### Health Checks

The existing health check endpoint reflects the default cluster:

- `GET /management/healthcheck`
- `GET /management/clusterhealth`

### Config Endpoint

The `_elasticConfig` endpoint shows default cluster info:

- `GET /_elasticConfig`
