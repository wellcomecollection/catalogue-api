variable "environment_name" {
  type = string
}

variable "image_uri" {
  type        = string
  description = "The image the Lambda is created with. The deploy workflow moves it after that."
}
