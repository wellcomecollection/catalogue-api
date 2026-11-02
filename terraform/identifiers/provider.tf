provider "aws" {
  region = "eu-west-1"

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

  region = "eu-west-1"

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
  alias = "identity"

  region = "eu-west-1"

  assume_role {
    role_arn = "arn:aws:iam::770700576653:role/identity-developer"
  }

  default_tags {
    tags = {
      "github.com/project-slug" = "wellcomecollection/catalogue-api"
    }
  }
}

provider "aws" {
  alias = "digirati"

  region = "eu-west-1"

  assume_role {
    role_arn = "arn:aws:iam::653428163053:role/digirati-developer"
  }

  default_tags {
    tags = {
      "github.com/project-slug" = "wellcomecollection/catalogue-api"
    }
  }
}
