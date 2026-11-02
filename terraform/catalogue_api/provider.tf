provider "aws" {
  region = var.aws_region

  assume_role {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
  }

  default_tags {
    tags = {
      "github.com/project-slug" = "wellcomecollection/catalogue-api"
    }
  }
}

provider "aws" {
  alias = "dns"

  region = var.aws_region

  assume_role {
    role_arn = "arn:aws:iam::267269328833:role/wellcomecollection-assume_role_hosted_zone_update"
  }

  default_tags {
    tags = {
      "github.com/project-slug" = "wellcomecollection/catalogue-api"
    }
  }
}

provider "aws" {
  alias = "experience"

  region = var.aws_region

  assume_role {
    role_arn = "arn:aws:iam::130871440101:role/experience-developer"
  }

  default_tags {
    tags = {
      "github.com/project-slug" = "wellcomecollection/catalogue-api"
    }
  }
}
