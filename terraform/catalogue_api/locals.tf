locals {
  catalogue_vpcs = data.terraform_remote_state.accounts_catalogue.outputs

  vpc_id          = local.catalogue_vpcs["catalogue_vpc_id"]
  private_subnets = local.catalogue_vpcs["catalogue_vpc_private_subnets"]

  elastic_cloud_vpce_sg_id = data.terraform_remote_state.infra_critical.outputs["ec_catalogue_privatelink_sg_id"]

  search_repository   = data.terraform_remote_state.catalogue_api_shared.outputs["ecr_search_repository_url"]
  items_repository    = data.terraform_remote_state.catalogue_api_shared.outputs["ecr_items_repository_url"]
  concepts_repository = data.terraform_remote_state.catalogue_api_shared.outputs["ecr_concepts_repository_url"]

  // The monitoring stack has had a Slack alert lambda listening on this
  // topic since it was created, but nothing ever published to it: no
  // CloudWatch alarm existed for the catalogue API, which is why the
  // July 2026 search outage (5xx rate above 90%) alerted nobody.
  api_gateway_alerts_topic_arn = data.terraform_remote_state.monitoring.outputs["catalogue_api_gateway_alerts_topic_arn"]

  // Raw CloudWatch alarm notifications rendered in Slack by AWS Chatbot,
  // used for alarms (eg latency) that the 5xx Slack lambda would describe
  // misleadingly as "errors in the API".
  chatbot_topic_arn = data.terraform_remote_state.monitoring.outputs["chatbot_topic_arn"]

  apm_secret_config = {
    apm_server_url = "catalogue/api/apm_server_url"
    apm_secret     = "catalogue/api/apm_secret"
  }

  // TODO: Requests & Items APIs have different security profile
  // TODO: Requests will access PII - and must have a different set of credentials!
  // See: https://github.com/wellcomecollection/catalogue-api/issues/95
  sierra_secret_config = {
    sierra_api_key    = "stacks/prod/sierra_api_key"
    sierra_api_secret = "stacks/prod/sierra_api_secret"
  }
}
