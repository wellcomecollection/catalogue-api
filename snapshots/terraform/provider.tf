provider "aws" {
  region = "eu-west-1"

  assume_role {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
  }

  ignore_tags {
    keys = ["deployment:label"]
  }

  default_tags {
    tags = {
      TerraformConfigurationURL = "https://github.com/wellcomecollection/catalogue-api/tree/main/snapshots/terraform"
      Environment               = "Production"
      Department                = "Digital Platform"
      Division                  = "Culture and Society"
      Use                       = "Catalogue Snapshots"
    }
  }
}
