terraform {
  backend "s3" {
    assume_role = {
      role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
    }

    bucket = "wellcomecollection-catalogue-infra-delta"
    key    = "terraform/catalogue-api/identifiers.tfstate"
    region = "eu-west-1"

    use_lockfile = true
  }
}

data "terraform_remote_state" "infra_critical" {
  backend = "s3"

  config = {
    assume_role = {
      role_arn = "arn:aws:iam::760097843905:role/platform-read_only"
    }

    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/catalogue/infrastructure/critical.tfstate"
    region = "eu-west-1"
  }
}
