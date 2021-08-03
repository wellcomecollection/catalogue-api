locals {
  default_tags = {
    TerraformConfigurationURL = "https://github.com/wellcomecollection/catalogue-api/tree/main/terraform/shared"
    Environment               = "Production"
    Department                = "Digital Platform"
    Division                  = "Culture and Society"
    Use                       = "Catalogue API"
  }
}

provider "aws" {
  region = var.aws_region

  assume_role {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
  }

  default_tags {
    tags = local.default_tags
  }
}

provider "aws" {
  alias = "platform"

  region = var.aws_region

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-developer"
  }

  default_tags {
    tags = local.default_tags
  }
}

provider "aws" {
  alias = "identity"

  region = var.aws_region

  assume_role {
    role_arn = "arn:aws:iam::770700576653:role/identity-developer"
  }

  default_tags {
    tags = local.default_tags
  }
}

provider "aws" {
  alias = "dns"

  region = var.aws_region

  assume_role {
    role_arn = "arn:aws:iam::267269328833:role/wellcomecollection-assume_role_hosted_zone_update"
  }

  default_tags {
    tags = local.default_tags
  }
}
