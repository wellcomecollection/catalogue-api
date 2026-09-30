terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"

      configuration_aliases = [
        aws.dns,
        aws.identity,
        aws.digirati,
      ]
    }
  }
}
