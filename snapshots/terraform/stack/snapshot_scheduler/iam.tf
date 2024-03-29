resource "aws_iam_role_policy" "snapshot_scheduler_sns_publish" {
  role   = module.snapshot_scheduler.role_name
  policy = module.scheduler_topic.publish_policy
}
