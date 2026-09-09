terraform {
  backend "s3" {
    assume_role = {
      role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
    }

    bucket         = "wellcomecollection-catalogue-infra-delta"
    key            = "terraform/catalogue/api/shared.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}
