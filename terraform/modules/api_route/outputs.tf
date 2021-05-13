output "resource_id" {
  value = aws_api_gateway_resource.resource.id
}

output "all_ids" {
  value = [
    aws_api_gateway_resource.resource.id,
    aws_api_gateway_method.method.id,
    aws_api_gateway_integration.integration.id
  ]
}
