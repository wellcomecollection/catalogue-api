# Multi-Cluster Elasticsearch Support

This document describes how to configure the Search API to read from multiple Elasticsearch clusters.

# ============================================================================

# Routes will be available at:

# ============================================================================

# Default cluster (existing):

# GET /works

# GET /works/:id

# GET /images

# GET /images/:id

#

# Experimental cluster A:

# GET /works/xp-a

# GET /works/xp-a/:id

#

# Experimental cluster B:

# GET /works/xp-b

# GET /works/xp-b/:id

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

## Setup Instructions

### Step 1: Store API Keys in AWS Secrets Manager

For custom clusters, you need to store the API key in Secrets Manager:

```bash
# Create the secret for your serverless cluster
aws secretsmanager create-secret \
  --name elasticsearch/xp-a/api_key \
  --description "API key for experimental serverless ES cluster" \
  --secret-string "your-base64-encoded-api-key"
```

For serverless Elasticsearch:

1. Go to your Elasticsearch Serverless project
2. Create an API key with appropriate permissions
3. Copy the base64-encoded key
4. Store it in AWS Secrets Manager

### Step 2: Configure the Cluster

Add configuration to your `application.conf` or environment variables as shown above.

### Step 3: Update Deployment

To enable multi-cluster support, update your deployment configuration to use the multi-cluster main class.

#### For SBT

In your `build.sbt` or run command:

```scala
mainClass in Compile := Some("weco.api.search.MultiClusterMain")
```

#### For Docker

Update your Dockerfile or docker-compose.yml:

```dockerfile
CMD ["java", "-cp", "...", "weco.api.search.MultiClusterMain"]
```

#### For Terraform

Update the task definition to use the new main class.

### Step 4: Deploy and Test

After deployment, your API will have new routes available:

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

## Example: Setting Up a Serverless Cluster

### 1. Create Elasticsearch Serverless Project

In AWS Console or via API, create a new Elasticsearch Serverless project:

- Project name: `catalogue-xp-a`
- Region: `us-east-1`

### 2. Get Connection Details

From the project overview:

- Endpoint: `https://catalogue-xp-a-abc123.us-east-1.es.aws.amazon.com`
- Note the hostname: `catalogue-xp-a-abc123.us-east-1.es.aws.amazon.com`

### 3. Create API Key

In the project security settings:

1. Create API key with read access to your indices
2. Copy the base64-encoded key

### 4. Store in Secrets Manager

```bash
aws secretsmanager create-secret \
  --name elasticsearch/xp-a/api_key \
  --secret-string "your-api-key-here"
```

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

### 6. Deploy and Test

```bash
# List works from experimental cluster
curl https://api.wellcomecollection.org/catalogue/v2/works/xp-a

# Get specific work
curl https://api.wellcomecollection.org/catalogue/v2/works/xp-a/abc123
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

### Logs

The system logs cluster initialization:

```
[INFO] Found multi-cluster configuration for clusters: xp-a, xp-b
[INFO] Parsed cluster config 'xp-a': pipelineDate=None, worksIndex=Some(works-xp-a), customHost=Some(...)
[INFO] Initializing client for cluster: xp-a
[INFO] Building custom Elasticsearch client for cluster 'xp-a' at https://...
[INFO] Using multi-cluster router with 2 additional cluster(s)
```

### Health Checks

The existing health check endpoint reflects the default cluster:

- `GET /management/healthcheck`
- `GET /management/clusterhealth`

### Config Endpoint

The `_elasticConfig` endpoint shows default cluster info:

- `GET /_elasticConfig`

## Security Considerations

1. **API Keys**: Store all API keys in AWS Secrets Manager, never in code
2. **IAM Permissions**: Ensure the API has permission to read secrets
3. **Network**: Ensure the API can reach all configured clusters
4. **Access Control**: Experimental endpoints are public - consider adding authentication if needed

## Performance Considerations

1. **Connection Pooling**: Each cluster gets its own connection pool
2. **Resilient Clients**: All clients use `ResilientElasticClient` for automatic retry on auth errors
3. **Resource Usage**: Multiple clients increase memory usage slightly
4. **Network Latency**: Different clusters may have different latencies

## Troubleshooting

### "Cluster 'xp-a' is not configured"

- Check that configuration is present in application.conf or environment variables
- Verify the cluster name matches exactly (case-sensitive)
- Check logs for parsing errors

### "ClusterConfig 'xp-a' must have either pipelineDate or custom connection details"

- Ensure you've set either `pipelineDate` OR the custom connection fields
- Don't mix pipeline and custom configurations for the same cluster

### Connection errors

- Verify hostname is correct and reachable
- Check API key is valid in Secrets Manager
- Ensure IAM role has permission to read the secret
- Verify network connectivity to the cluster

### Index not found

- Confirm the index exists in the cluster
- Check index name spelling in configuration
- Verify API key has permission to access the index
