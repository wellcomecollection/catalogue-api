resource "github_actions_secret" "identifiers_ci" {
  repository      = "catalogue-api"
  secret_name     = "IDENTIFIERS_CI_ROLE_ARN"
  plaintext_value = module.gha_identifiers_ci_role.role_arn
}
