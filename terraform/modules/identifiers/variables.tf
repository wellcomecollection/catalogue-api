variable "environment_name" {
  type = string
}

variable "image_uri" {
  type = string
}

variable "openapi_spec_path" {
  type = string
}

variable "registry_cluster_arn" {
  type = string
}

variable "registry_read_role_arn" {
  type = string
}

variable "registry_secret_arn" {
  type = string
}

variable "api_gateway_alerts_topic_arn" {
  type        = string
  description = "SNS topic that routes API Gateway 5xx alarms to Slack via the monitoring stack"
}

variable "lambda_error_alerts_topic_arn" {
  type        = string
  description = "SNS topic that routes Lambda error alarms to Slack via the monitoring stack"
}

variable "chatbot_topic_arn" {
  type        = string
  description = "SNS topic rendered in Slack by AWS Chatbot, for alarms without a bespoke Slack lambda"
}

variable "enable_api_alarms" {
  type        = bool
  default     = false
  description = "Off for stage, so testing there doesn't alert the shared Slack channel"
}
