locals {
  elasticsearch_apps_catalogue  = ["search", "items"]
  elasticsearch_apps_identity  = ["requests"]

  elasticsearch_creds = ["es_username", "es_password", "es_protocol", "es_port"]


}

resource "aws_secretsmanager_secret" "es_credentials" {
  for_each = toset([
    for path in setproduct(local.elasticsearch_apps_catalogue, local.elasticsearch_creds) : "${path[0]}/${path[1]}"
  ])
  name = "catalogue/${each.value}"
}

resource "aws_secretsmanager_secret" "es_credentials_identity" {
  for_each = toset([
  for path in setproduct(local.elasticsearch_apps_identity, local.elasticsearch_creds) : "${path[0]}/${path[1]}"
  ])
  name = "catalogue/${each.value}"

  provider = "aws.identity"
}
