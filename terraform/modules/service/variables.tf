variable "container_image" {
  type = string
}

variable "service_name" {
  type = string
}

variable "environment" {
  type    = map(string)
  default = {}
}

variable "secrets" {
  type    = map(string)
  default = {}
}

variable "container_port" {
  type    = number
  default = 9001
}

variable "service_discovery_namespace_id" {
  type    = string
  default = null
}

variable "use_fargate_spot" {
  type    = bool
  default = false
}

variable "turn_off_outside_office_hours" {
  type    = bool
  default = false
}

variable "cluster_arn" {
  type = string
}

variable "subnets" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "desired_task_count" {
  type    = number
  default = 3
}

variable "vpc_id" {
  type = string
}

variable "load_balancer_arn" {
  type = string
}

variable "load_balancer_listener_port" {
  type = number
}

variable "app_cpu" {
  type = number
}

variable "app_memory" {
  type = number
}

variable "healthcheck_path" {
  type    = string
  default = "/management/healthcheck"
}

