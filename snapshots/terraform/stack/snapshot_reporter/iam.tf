data "aws_iam_policy_document" "read_secrets" {
  statement {
    actions = [
      "secretsmanager:GetSecretValue",
    ]

    resources = [
      for _, secret in data.aws_secretsmanager_secret_version.secrets : secret.arn
    ]
  }
}

resource "aws_iam_role_policy" "snapshot_reporter_read_secrets" {
  role   = module.snapshot_reporter.role_name
  policy = data.aws_iam_policy_document.read_secrets.json
}
