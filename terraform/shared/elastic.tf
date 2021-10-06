resource "ec_deployment" "catalogue_api" {
  name = "catalogue-api"

  region                 = "eu-west-1"
  version                = "7.14.2"
  deployment_template_id = "aws-io-optimized-v2"

  traffic_filter = local.catalogue_ec_traffic_filter

  elasticsearch {
    topology {
      id         = "hot_content"
      size       = "8g"
      zone_count = 3
    }

    config {
      user_settings_yaml = yamlencode(
        {
          # This setting is a fix for performance problems with an optimisation added in 7.13.1
          # See: https://www.elastic.co/guide/en/elasticsearch/reference/7.13/release-notes-7.13.2.html
          # "Add setting to disable aggs optimization #73620 (issue: #73426)"
          # TODO: A fix is likely in ES 7.14, after which we can remove this setting.
          "search.aggs.rewrite_to_filter_by_filter" : true
        }
      )
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
    deployment_id = local.logging_cluster_id
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

  # These are used for creating users in scripts/create_elastic_roles.py
  cluster_elastic_user_secrets = {
    "elasticsearch/catalogue_api/username" = local.catalogue_elastic_username
    "elasticsearch/catalogue_api/password" = local.catalogue_elastic_password
  }

  cluster_secrets = {
    "elasticsearch/catalogue_api/public_host"  = local.catalogue_public_host
    "elasticsearch/catalogue_api/private_host" = local.catalogue_private_host
    "elasticsearch/catalogue_api/protocol"     = "https"
    "elasticsearch/catalogue_api/port"         = 9243
  }

  # This config will be consumed by the items service in the catalogue_api stack
  es_items_secret_config = {
    es_host     = "elasticsearch/catalogue_api/public_host"
    es_port     = "elasticsearch/catalogue_api/port"
    es_protocol = "elasticsearch/catalogue_api/protocol"
    es_username = module.items_elastic_user.aws_secretsmanager_secret_username_name
    es_password = module.items_elastic_user.aws_secretsmanager_secret_password_name
  }

  # This config will be consumed by the requests service in the identity stack
  es_requests_secret_config = {
    es_host     = "elasticsearch/catalogue_api/public_host"
    es_port     = "elasticsearch/catalogue_api/port"
    es_protocol = "elasticsearch/catalogue_api/protocol"
    es_username = module.requests_elastic_user.aws_secretsmanager_secret_username_name
    es_password = module.requests_elastic_user.aws_secretsmanager_secret_password_name
  }

  # This config will be consumed by the search service in the catalogue_api stack
  es_search_secret_config = {
    es_host     = "elasticsearch/catalogue_api/public_host"
    es_port     = "elasticsearch/catalogue_api/port"
    es_protocol = "elasticsearch/catalogue_api/protocol"
    es_username = module.search_elastic_user.aws_secretsmanager_secret_username_name
    es_password = module.search_elastic_user.aws_secretsmanager_secret_password_name
  }
}

# Cluster secrets

## Elastic user details

module "elastic_user_secrets" {
  source = "github.com/wellcomecollection/terraform-aws-secrets.git?ref=v1.2.0"

  key_value_map = local.cluster_elastic_user_secrets
}

## Cluster host details - catalogue account

module "catalogue_api_secrets" {
  source = "github.com/wellcomecollection/terraform-aws-secrets.git?ref=v1.2.0"

  key_value_map = local.cluster_secrets
}

## Cluster host details - identity account

module "identity_secrets" {
  source = "github.com/wellcomecollection/terraform-aws-secrets.git?ref=v1.2.0"

  key_value_map = local.cluster_secrets

  providers = {
    aws = aws.identity
  }
}

# Elastic users

module "search_elastic_user" {
  source  = "../modules/elastic_user"
  service = "search"
  roles   = ["catalogue_read"]
}

module "items_elastic_user" {
  source  = "../modules/elastic_user"
  service = "items"
  roles   = ["catalogue_read"]
}

module "requests_elastic_user" {
  source  = "../modules/elastic_user"
  service = "requests"
  roles   = ["catalogue_read"]

  providers = {
    aws = aws.identity
  }
}

module "replication_manager_elastic_user" {
  source  = "../modules/elastic_user"
  service = "replication_manager"
  roles   = ["catalogue_read", "catalogue_manage_ccr"]
}

module "diff_tool_elastic_user" {
  source  = "../modules/elastic_user"
  service = "diff_tool"
  roles   = ["catalogue_read"]
}

module "internal_model_tool_elastic_user" {
  source  = "../modules/elastic_user"
  service = "internal_model_tool"
  roles   = ["catalogue_read"]
}
