resource "github_actions_secret" "identifiers_ci" {
  repository      = "catalogue-api"
  secret_name     = "IDENTIFIERS_CI_ROLE_ARN"
  plaintext_value = module.gha_identifiers_ci_role.role_arn
}

resource "github_actions_secret" "items_v3_ci" {
  repository      = "catalogue-api"
  secret_name     = "ITEMS_V3_CI_ROLE_ARN"
  plaintext_value = module.gha_items_v3_ci_role.role_arn
}
