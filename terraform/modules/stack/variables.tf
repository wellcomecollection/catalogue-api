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
    items  = number
  })
}

variable "container_images" {
  type = object({
    search = string,
    items  = string
  })
}

variable "es_items_secret_config" {
  type = object({
    es_host     = string
    es_port     = string
    es_protocol = string
    es_username = string
    es_password = string
  })
}

variable "es_search_secret_config" {
  type = object({
    es_host     = string
    es_port     = string
    es_protocol = string
    es_username = string
    es_password = string
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
