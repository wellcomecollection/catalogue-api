output "ecr_api_repository_url" {
  value = aws_ecr_repository.api.repository_url
}

output "ecr_search_repository_url" {
  value = aws_ecr_repository.search.repository_url
}

output "ecr_snapshot_generator_repository_url" {
  value = aws_ecr_repository.snapshot_generator.repository_url
}

output "ecr_items_repository_url" {
  value = aws_ecr_repository.items.repository_url
}

output "ecr_requests_repository_url" {
  value = aws_ecr_repository.requests.repository_url
}
