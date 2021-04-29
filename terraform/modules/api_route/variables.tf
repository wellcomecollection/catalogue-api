variable "rest_api_id" {
  type = string
}

variable "parent_id" {
  type = string
}

variable "vpc_link_id" {
  type = string
}

variable "path_part" {
  type = string
}

variable "path_param" {
  type    = string
  default = null
}

variable "http_method" {
  type = string
}

variable "lb_port" {
  type = number
}

variable "integration_path" {
  type = string
}

variable "external_hostname" {
  type = string
}
