# Keeps every image a deployment can reference (env.* and latest tags), then only the newest 10 others,
# so Amazon Inspector scans what we run rather than years of CI builds. The identifiers Lambdas resolve a
# commit tag at deploy, which the newest 10 cover.
locals {
  ecr_lifecycle_policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep env tagged images, e.g. env.prod or env.stage"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["env."]
          countType     = "imageCountMoreThan"
          countNumber   = 20
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 2
        description  = "Keep the latest tagged image"
        selection = {
          tagStatus     = "tagged"
          tagPrefixList = ["latest"]
          countType     = "imageCountMoreThan"
          countNumber   = 1
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 3
        description  = "Expire other tagged images, keep only the last 10"
        selection = {
          tagStatus      = "tagged"
          tagPatternList = ["*"]
          countType      = "imageCountMoreThan"
          countNumber    = 10
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 4
        description  = "Expire untagged images, keep only the last 5"
        selection = {
          tagStatus   = "untagged"
          countType   = "imageCountMoreThan"
          countNumber = 5
        }
        action = { type = "expire" }
      }
    ]
  })

  catalogue_ecr_repositories = {
    items              = aws_ecr_repository.items.name
    search             = aws_ecr_repository.search.name
    concepts           = aws_ecr_repository.concepts.name
    identifiers        = aws_ecr_repository.identifiers.name
    nginx_api_gw       = aws_ecr_repository.nginx_api_gw.name
    snapshot_generator = aws_ecr_repository.snapshot_generator.name
  }
}

resource "aws_ecr_lifecycle_policy" "catalogue" {
  for_each = local.catalogue_ecr_repositories

  repository = each.value
  policy     = local.ecr_lifecycle_policy
}

resource "aws_ecr_lifecycle_policy" "requests" {
  provider = aws.identity

  repository = aws_ecr_repository.requests.name
  policy     = local.ecr_lifecycle_policy
}
