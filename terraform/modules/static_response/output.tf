output "resource_id" {
  value = aws_api_gateway_resource.static.id
}

output "all_ids" {
  value = [
    aws_api_gateway_resource.static.id,
    aws_api_gateway_method.static.id,
    aws_api_gateway_integration.static.id,
    aws_api_gateway_method_response.static.id,
    aws_api_gateway_integration_response.static.id
  ]
}
