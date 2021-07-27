data "aws_secretsmanager_secret_version" "secrets" {
  for_each = toset(local.secrets)

  secret_id = each.key
}

locals {
  elastic_secret_id = "catalogue/snapshots/read_user"
  slack_secret_id   = "snapshot_reporter/slack_webhook"

  secrets = [
    local.elastic_secret_id,
    local.slack_secret_id,
    "elasticsearch/catalogue_api/search/username",
    "elasticsearch/catalogue_api/search/password",
    "elasticsearch/catalogue_api/public_host",
  ]
}
