resource "aws_iam_role_policy" "service_get_secrets" {
  role   = module.task_definition.task_role_name
  policy = data.aws_iam_policy_document.get_secrets.json
}

data "aws_iam_policy_document" "get_secrets" {
  statement {
    actions = [
      "secretsmanager:GetSecretValue",
    ]

    resources = [
      "arn:aws:secretsmanager:eu-west-1:756629837203:secret:elasticsearch/*",
      "arn:aws:secretsmanager:eu-west-1:756629837203:secret:stacks/*",
    ]
  }
}
