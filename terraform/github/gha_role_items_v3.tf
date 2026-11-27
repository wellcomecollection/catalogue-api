module "gha_items_v3_ci_role" {
  source = "github.com/wellcomecollection/terraform-aws-gha-role?ref=v2.1.0"

  policy_document = data.aws_iam_policy_document.gha_items_v3_ci.json
  role_name       = "items-v3-ci"

  # Scoped to these rather than the whole repository, so pull request runs
  # cannot assume it. The deploy job names an environment, and presents
  # environment:<name> instead of the ref, so the environment below has to
  # refuse other branches itself.
  github_repositories = [
    "wellcomecollection/catalogue-api:ref:refs/heads/main",
    "wellcomecollection/catalogue-api:environment:items-v3-stage",
  ]

  github_oidc_provider_arn = data.terraform_remote_state.catalogue_account.outputs.github_openid_connect_provider_arn
}

data "aws_iam_policy_document" "gha_items_v3_ci" {
  statement {
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:Describe*",
      "ecr:Get*",
      "ecr:List*",
      "ecr:TagResource",
      "ecr:PutImage",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
    ]
    resources = [
      "arn:aws:ecr:eu-west-1:756629837203:repository/uk.ac.wellcome/items_v3",
    ]
  }

  statement {
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    actions = [
      "lambda:GetFunctionConfiguration",
      "lambda:UpdateFunctionCode",
    ]
    resources = [
      "arn:aws:lambda:eu-west-1:756629837203:function:items-v3-api-stage",
    ]
  }
}

# Created here, not by the first deploy, which would make it with no branch
# policy. A job on any branch could then name it and assume the role.
resource "github_repository_environment" "items_v3_stage" {
  repository  = "catalogue-api"
  environment = "items-v3-stage"

  deployment_branch_policy {
    protected_branches     = false
    custom_branch_policies = true
  }
}

resource "github_repository_environment_deployment_policy" "items_v3_stage_main" {
  repository     = "catalogue-api"
  environment    = github_repository_environment.items_v3_stage.environment
  branch_pattern = "main"
}
