module "gha_identifiers_ci_role" {
  source = "github.com/wellcomecollection/terraform-aws-gha-role?ref=v2.1.0"

  policy_document = data.aws_iam_policy_document.gha_identifiers_ci.json
  role_name       = "identifiers-ci"

  # Scoped to these three rather than the whole repository, which would take
  # pull request runs with it. A job that names an environment presents
  # environment:<name> instead of the ref, so the publish job and the two deploy
  # jobs each present a different subject.
  github_repositories = [
    "wellcomecollection/catalogue-api:ref:refs/heads/main",
    "wellcomecollection/catalogue-api:environment:stage",
    "wellcomecollection/catalogue-api:environment:prod",
  ]

  github_oidc_provider_arn = data.terraform_remote_state.catalogue_account.outputs.github_openid_connect_provider_arn
}

data "aws_iam_policy_document" "gha_identifiers_ci" {
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
      "arn:aws:ecr:eu-west-1:756629837203:repository/uk.ac.wellcome/identifiers",
    ]
  }

  statement {
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  # The Lambda is created by platform#6531; granted here so that issue's deploy
  # job doesn't have to reopen this stack.
  statement {
    actions = [
      "lambda:GetFunctionConfiguration",
      "lambda:UpdateFunctionCode",
    ]
    resources = [
      "arn:aws:lambda:eu-west-1:756629837203:function:identifiers-api-*",
    ]
  }
}
