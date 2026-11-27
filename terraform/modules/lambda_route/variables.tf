variable "rest_api_id" {
  type = string
}

variable "rest_api_execution_arn" {
  type = string
}

variable "parent_id" {
  type = string
}

variable "path_part" {
  type = string
}

variable "http_method" {
  type = string
}

variable "api_key_required" {
  type    = bool
  default = false
}

variable "lambda_name" {
  type = string
}

variable "lambda_invoke_arn" {
  type = string
}
