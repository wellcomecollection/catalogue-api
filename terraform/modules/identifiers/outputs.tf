output "hostname" {
  value = aws_api_gateway_domain_name.identifiers.domain_name
}

output "invoke_url" {
  value = aws_api_gateway_stage.default.invoke_url
}

output "lambda_name" {
  value = module.identifiers_lambda.lambda.function_name
}
