resource "aws_iam_role_policy" "search_get_secrets" {
  role   = module.search_api.task_role_name
  policy = data.aws_iam_policy_document.get_secrets.json
}

data "aws_iam_policy_document" "get_secrets" {
  statement {
    actions = [
      "secretsmanager:GetSecretValue",
    ]

    resources = [
      "arn:aws:secretsmanager:eu-west-1:756629837203:secret:elasticsearch/*",
    ]
  }
}
