provider "aws" {
  region = "eu-west-1"

  assume_role {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
  }
}

# Configure the GitHub Provider
# Create a fine-grained personal access token in Github:
# Go to your Github account > Settings > Developer settings > PAT > Fine-grained tokens
# Give it a name, description and a short (7 days) expiration
# Under Repository permissions, give it access to catalogue-api and set
# Secrets: read and write. gha_secret.tf creates a repository secret, so the
# organization Secrets permission is not the one needed.
# export TF_VAR_github_token=<your-token-here> before applying the tf
provider "github" {
  owner = "wellcomecollection"
  token = var.github_token
}

variable "github_token" {
  type      = string
  sensitive = true
}
