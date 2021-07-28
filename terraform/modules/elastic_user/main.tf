variable "service" {
  type = string
}

variable "roles" {
  type = list(string)
}

resource "aws_secretsmanager_secret" "username" {
  name = "elasticsearch/catalogue_api/${var.service}/username"
  description = "Config secret populated by Terraform"
}

resource "aws_secretsmanager_secret" "password" {
  name = "elasticsearch/catalogue_api/${var.service}/password"
  description = "Config secret populated by Terraform"
}

resource "aws_secretsmanager_secret_version" "username" {
  secret_id     = aws_secretsmanager_secret.username.id
  secret_string = var.service
}

resource "aws_secretsmanager_secret_version" "password" {
  secret_id     = aws_secretsmanager_secret.username.id
  secret_string = random_password.password.result

  provisioner "local-exec" {
    command = "python3 ../modules/elastic_user/create_user.py ${var.service} ${random_password.password.result} ${join(",", var.roles)}"
  }
}

resource "random_password" "password" {
  length           = 16
  special          = true
  override_special = "_%@"
}

resource "null_resource" "roles" {
  triggers = {
    roles = join(",", var.roles)
  }

  depends_on = [aws_secretsmanager_secret_version.password]
  provisioner "local-exec" {
    command = "python3 ../modules/elastic_user/update_user.py ${var.service} ${join(",", var.roles)}"
  }
}
