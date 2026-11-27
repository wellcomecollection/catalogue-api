output "lambda_name" {
  value = module.lambda.lambda.function_name
}

output "invoke_arn" {
  value = module.lambda.lambda.invoke_arn
}
