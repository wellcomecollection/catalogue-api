# Multi-Cluster Quick Start

## TL;DR

Add config for your serverless/experimental cluster, deploy with `MultiClusterMain`, get new routes.

## Minimal Serverless Setup

### 1. Get your serverless ES details

- Hostname: `my-project.es.us-east-1.aws.elastic.cloud`
- API Key: (base64 encoded)
- Index name: `works-experimental`

### 2. Store API key

```bash
aws secretsmanager create-secret \
  --name elasticsearch/xp-a/api_key \
  --secret-string "your-api-key-base64"
```

### 3. Add to application.conf

```hocon
multiCluster.xp-a {
  customHost = "my-project.es.us-east-1.aws.elastic.cloud"
  customApiKeySecretPath = "elasticsearch/xp-a/api_key"
  worksIndex = "works-experimental"
}
```

### 4. Use MultiClusterMain

Update your deployment to use `weco.api.search.MultiClusterMain` instead of `weco.api.search.Main`.

### 5. Test

```bash
curl https://your-api/catalogue/v2/works/xp-a
curl https://your-api/catalogue/v2/works/xp-a/{work-id}
```

## Adding More Routes

Edit `MultiClusterSearchApi.scala` to add routes like `/works/xp-b/`, `/images/xp-a/`, etc.

## Files Created

- `common/search/.../models/MultiClusterElasticConfig.scala` - Config models
- `common/search/.../config/builders/MultiClusterElasticClientBuilder.scala` - Client builder
- `search/.../MultiClusterSearchApi.scala` - Multi-cluster router
- `search/.../config/MultiClusterConfigParser.scala` - Config parser
- `search/.../MultiClusterMain.scala` - Main entry point
- `docs/multi-cluster-setup.md` - Full documentation

## Example Config

```hocon
# Serverless cluster
multiCluster.xp-a {
  customHost = "serverless.es.aws.elastic.cloud"
  customApiKeySecretPath = "elasticsearch/xp-a/api_key"
  worksIndex = "works-xp-a"
}

# Another pipeline cluster
multiCluster.xp-b {
  pipelineDate = "2025-12-01"
  worksIndex = "works-indexed-2025-12-01"
}
```

## Routes Generated

- `/works/xp-a` → xp-a cluster works
- `/works/xp-a/{id}` → xp-a cluster single work
- `/works/xp-b` → xp-b cluster works
- `/works/xp-b/{id}` → xp-b cluster single work
- `/works` → default cluster (unchanged)
- `/works/{id}` → default cluster (unchanged)
