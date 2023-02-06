variable "lambda_error_alarm_arn" {
  type = string
}

variable "lambda_upload_bucket" {
  type = string
}

variable "public_bucket_name" {
  type = string
}

variable "public_object_key_prefix" {
  type = string
}

variable "es_bulk_size" {
  type = number
}

variable "deployment_service_env" {
  type = string
}
