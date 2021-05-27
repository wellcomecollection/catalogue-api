variable "key_value_map" {
  type = map(string)
}

variable "description" {
  default = null
}

variable "tags" {
  type    = map(string)
  default = null
}
