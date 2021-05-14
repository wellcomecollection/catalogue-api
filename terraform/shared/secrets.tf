locals {
  elasticsearch_apps  = ["search", "items"]
  elasticsearch_creds = ["es_username", "es_password", "es_protocol", "es_port"]
}

resource "aws_secretsmanager_secret" "es_credentials" {
  for_each = toset([
    for path in setproduct(local.elasticsearch_apps, local.elasticsearch_creds) : "${path[0]}/${path[1]}"
  ])
  name = "catalogue/${each.value}"
}
