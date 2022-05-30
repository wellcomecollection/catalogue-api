variable "environment_name" {
  type = string
}

variable "cluster_arn" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "external_hostname" {
  type = string
}

variable "private_subnets" {
  type = list(string)
}

variable "elastic_cloud_vpce_sg_id" {
  type = string
}

variable "desired_task_counts" {
  type = object({
    search = number,
    items  = number,
    concepts = number
  })
}

variable "container_images" {
  type = object({
    search = string,
    items  = string,
    concepts = string
  })
}

variable "apm_secret_config" {
  type = object({
    apm_server_url = string
    apm_secret     = string
  })
}

variable "sierra_secret_config" {
  type = object({
    sierra_api_key    = string
    sierra_api_secret = string
  })
}

variable "api_gateway_alerts_topic_arn" {
  type = string
}
