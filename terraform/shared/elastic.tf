data "ec_deployment" "logging" {
  id = local.logging_cluster_id
}

resource "ec_deployment" "catalogue_api" {
  name = "catalogue-api"

  region                 = "eu-west-1"
  version                = "7.13.2"
  deployment_template_id = "aws-io-optimized-v2"

  traffic_filter = local.catalogue_ec_traffic_filter

  elasticsearch {
    topology {
      id         = "hot_content"
      size       = "8g"
      zone_count = 3
    }
  }

  # The catalogue-api cluster gets the pipeline-storage clusters added
  # as remote clusters dynamically.  We don't want these to be rolled
  # back when we change the Terraform, so ignore any changes here.
  lifecycle {
    ignore_changes = [elasticsearch[0].remote_cluster]
  }

  kibana {
    topology {
      zone_count = 1
      size       = "1g"
    }
  }

  observability {
    deployment_id = data.ec_deployment.logging.id
  }
}

locals {
  catalogue_api_elastic_id   = ec_deployment.catalogue_api.elasticsearch[0].resource_id
  catalogue_elastic_region   = ec_deployment.catalogue_api.elasticsearch[0].region
  catalogue_elastic_username = ec_deployment.catalogue_api.elasticsearch_username
  catalogue_elastic_password = ec_deployment.catalogue_api.elasticsearch_password

  # See https://www.elastic.co/guide/en/cloud/current/ec-traffic-filtering-vpc.html
  catalogue_private_host = "${local.catalogue_elastic_region}.vpce.${local.catalogue_elastic_region}.aws.elastic-cloud.com"
  catalogue_public_host  = "${local.catalogue_api_elastic_id}.${local.catalogue_elastic_region}.aws.found.io"

  catalogue_api_secrets = {
    "elasticsearch/catalogue_api/public_host"  = local.catalogue_public_host
    "elasticsearch/catalogue_api/private_host" = local.catalogue_private_host

    "elasticsearch/catalogue_api/username" = local.catalogue_elastic_username
    "elasticsearch/catalogue_api/password" = local.catalogue_elastic_password
    "elasticsearch/catalogue_api/protocol" = "https"
    "elasticsearch/catalogue_api/port"     = 9243
  }

  # This config will be consumed by the items service in the catalogue_api stack
  es_items_secret_config = {
    es_host     = "elasticsearch/catalogue_api/public_host"
    es_port     = "elasticsearch/catalogue_api/port"
    es_protocol = "elasticsearch/catalogue_api/protocol"
    es_username = aws_secretsmanager_secret.service-items-username.name
    es_password = aws_secretsmanager_secret.service-items-password.name
  }

  # This config will be consumed by the requests service in the identity stack
  es_requests_secret_config = {
    es_host     = "elasticsearch/catalogue_api/public_host"
    es_port     = "elasticsearch/catalogue_api/port"
    es_protocol = "elasticsearch/catalogue_api/protocol"
    es_username = aws_secretsmanager_secret.service-requests-username.name
    es_password = aws_secretsmanager_secret.service-requests-password.name
  }

  # This config will be consumed by the search service in the catalogue_api stack
  es_search_secret_config = {
    es_host     = "elasticsearch/catalogue_api/public_host"
    es_port     = "elasticsearch/catalogue_api/port"
    es_protocol = "elasticsearch/catalogue_api/protocol"
    es_username = aws_secretsmanager_secret.service-search-username.name
    es_password = aws_secretsmanager_secret.service-search-password.name
  }
}

# Cluster management secrets

module "catalogue_api_secrets" {
  source = "../modules/secrets"

  key_value_map = local.catalogue_api_secrets

  description = "Config secret populated by Terraform"
  tags        = local.default_tags
}

# Search service credentials

resource "aws_secretsmanager_secret" "service-search-username" {
  name = "elasticsearch/catalogue_api/search/username"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags
}

resource "aws_secretsmanager_secret" "service-search-password" {
  name = "elasticsearch/catalogue_api/search/password"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags
}

# Items service credentials

resource "aws_secretsmanager_secret" "service-items-username" {
  name = "elasticsearch/catalogue_api/items/username"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags
}

resource "aws_secretsmanager_secret" "service-items-password" {
  name = "elasticsearch/catalogue_api/items/password"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags
}

# Requests service credentials

resource "aws_secretsmanager_secret" "service-requests-username" {
  name = "elasticsearch/catalogue_api/requests/username"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags

  provider = "aws.identity"
}

resource "aws_secretsmanager_secret" "service-requests-password" {
  name = "elasticsearch/catalogue_api/requests/password"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags

  provider = "aws.identity"
}

# Diff tool service credentials

resource "aws_secretsmanager_secret" "service-diff_tool-username" {
  name = "elasticsearch/catalogue_api/diff_tool/username"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags
}

resource "aws_secretsmanager_secret" "service-diff_tool-password" {
  name = "elasticsearch/catalogue_api/diff_tool/password"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags
}

# Replication manager service credentials

resource "aws_secretsmanager_secret" "service-replication_manager-username" {
  name = "elasticsearch/catalogue_api/replication_manager/username"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags
}

resource "aws_secretsmanager_secret" "service-replication_manager-password" {
  name = "elasticsearch/catalogue_api/replication_manager/password"

  description = "Config secret populated by Terraform"
  tags        = local.default_tags
}

# Create users

resource "null_resource" "elasticsearch_users" {
  triggers = {
    pipeline_storage_elastic_id = ec_deployment.catalogue_api.elasticsearch[0].resource_id
  }

  depends_on = [
    ec_deployment.catalogue_api,
    module.catalogue_api_secrets,
    aws_secretsmanager_secret.service-search-username,
    aws_secretsmanager_secret.service-search-password,
    aws_secretsmanager_secret.service-items-username,
    aws_secretsmanager_secret.service-items-password,
    aws_secretsmanager_secret.service-requests-username,
    aws_secretsmanager_secret.service-requests-password,
    aws_secretsmanager_secret.service-diff_tool-username,
    aws_secretsmanager_secret.service-diff_tool-password,
    aws_secretsmanager_secret.service-replication_manager-username,
    aws_secretsmanager_secret.service-replication_manager-password,
  ]

  provisioner "local-exec" {
    command = "python3 scripts/create_elastic_users.py"
  }
}
