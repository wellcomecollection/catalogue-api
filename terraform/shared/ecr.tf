resource "aws_ecr_repository" "items" {
  name = "uk.ac.wellcome/items"

  lifecycle {
    prevent_destroy = true
  }
}

# This is in the identity account as it is deployed there
# TODO: Remove this when we are deploying it from identity
resource "aws_ecr_repository" "requests" {
  provider = aws.identity

  name = "uk.ac.wellcome/requests"

  lifecycle {
    prevent_destroy = true
  }
}

# TODO: Rename this (remove -new) by removing and importing state
resource "aws_ecr_repository" "requests-new" {
  name = "uk.ac.wellcome/requests"

  //  lifecycle {
  //    prevent_destroy = true
  //  }
}


resource "aws_ecr_repository" "search" {
  name = "uk.ac.wellcome/search"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_repository" "api" {
  name = "uk.ac.wellcome/api"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_repository" "nginx_api_gw" {
  name = "uk.ac.wellcome/nginx_api_gw"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_repository" "snapshot_generator" {
  name = "uk.ac.wellcome/snapshot_generator"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_repository" "update_api_docs" {
  name = "uk.ac.wellcome/update_api_docs"

  lifecycle {
    prevent_destroy = true
  }
}

# These are the ECR repositories in the *platform* account, where new images
# are currently published.  Eventually we should publish images into the catalogue
# account and remove these repositories, but that's not done yet.

resource "aws_ecr_repository" "platform_nginx_api_gw" {
  provider = aws.platform

  name = "uk.ac.wellcome/nginx_api-gw"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_repository" "platform_update_api_docs" {
  provider = aws.platform

  name = "uk.ac.wellcome/update_api_docs"

  lifecycle {
    prevent_destroy = true
  }
}
