module "gha_items_v3_ci_role" {
  source = "github.com/wellcomecollection/terraform-aws-gha-role?ref=v2.1.0"

  policy_document = data.aws_iam_policy_document.gha_items_v3_ci.json
  role_name       = "items-v3-ci"

  # Main only, so pull request runs cannot assume it. The deploy jobs will add
  # their environment subjects when the Lambda exists.
  github_repositories = [
    "wellcomecollection/catalogue-api:ref:refs/heads/main",
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
}
