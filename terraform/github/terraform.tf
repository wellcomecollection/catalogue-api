terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
    github = {
      source = "integrations/github"
    }
  }

  backend "s3" {
    assume_role = {
      role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
    }

    bucket = "wellcomecollection-catalogue-infra-delta"
    key    = "terraform/catalogue-api/ci.tfstate"
    region = "eu-west-1"

    # S3 native locking, rather than the deprecated dynamodb_table the other
    # roots still use. Locks are per state file, so this coordinates with
    # nothing else.
    use_lockfile = true
  }
}

# Exposes github_openid_connect_provider_arn for the GHA role.
data "terraform_remote_state" "catalogue_account" {
  backend = "s3"

  config = {
    assume_role = {
      role_arn = "arn:aws:iam::760097843905:role/platform-read_only"
    }

    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/aws-account-infrastructure/catalogue.tfstate"
    region = "eu-west-1"
  }
}
