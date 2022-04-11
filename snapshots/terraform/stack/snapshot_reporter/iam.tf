data "aws_iam_policy_document" "read_secrets" {
  statement {
    actions = [
      "secretsmanager:GetSecretValue",
    ]

    resources = [
      "arn:aws:secretsmanager:eu-west-1:756629837203:secret:${local.elastic_secret_id}*",
      "arn:aws:secretsmanager:eu-west-1:756629837203:secret:${local.slack_secret_id}*",
      "arn:aws:secretsmanager:eu-west-1:756629837203:secret:elasticsearch/*",
    ]
  }
}

resource "aws_iam_role_policy" "snapshot_reporter_read_secrets" {
  role   = module.snapshot_reporter.role_name
  policy = data.aws_iam_policy_document.read_secrets.json
}
