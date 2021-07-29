output "aws_secretsmanager_secret_username_name" {
  value = aws_secretsmanager_secret.username.name
}

output "aws_secretsmanager_secret_password_name" {
  value = aws_secretsmanager_secret.password.name
}