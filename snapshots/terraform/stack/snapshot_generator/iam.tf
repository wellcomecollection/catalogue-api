# Role policies for the snapshot_generator

resource "aws_iam_role_policy" "ecs_snapshot_generator_task_s3_public" {
  role   = module.snapshot_generator.task_role_name
  policy = data.aws_iam_policy_document.public_data_bucket_full_access_policy.json
}

resource "aws_iam_role_policy" "snapshot_generator_cloudwatch" {
  role   = module.snapshot_generator.task_role_name
  policy = data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json
}

resource "aws_iam_role_policy" "snapshot_generator_read_from_q" {
  role   = module.snapshot_generator.task_role_name
  policy = module.snapshot_generator_input_queue.read_policy
}

resource "aws_iam_role_policy" "snapshot_generator_get_secrets" {
  role   = module.snapshot_generator.task_role_name
  policy = data.aws_iam_policy_document.get_secrets.json
}

# Policy documents

data "aws_iam_policy_document" "allow_cloudwatch_push_metrics" {
  statement {
    actions = [
      "cloudwatch:PutMetricData",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "public_data_bucket_full_access_policy" {
  statement {
    actions = [
      "s3:Put*",
    ]

    resources = [
      "arn:aws:s3:::${var.public_bucket_name}/catalogue/*",
    ]
  }
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

