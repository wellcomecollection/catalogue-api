variable "environment_name" {
  type = string
}

variable "cluster_arn" {
  type = string
}

variable "vpc_id" {
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
    search_api = number,
    items_api = number
  })
}

variable "container_images" {
  type = object({
    search_api = string,
    items_api = string
  })
}
