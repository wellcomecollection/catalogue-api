# Digest-pinned. terraform-aws-lambda ignores image_uri changes, so this only
# sets the image the Lambda is created with; the deploy workflow moves it after.
data "aws_ecr_image" "items_v3" {
  repository_name = "uk.ac.wellcome/items_v3"
  most_recent     = true
}

# Stage only until the v3 path is agreed for production
# (wellcomecollection/platform#6932).
module "items_v3_stage" {
  source = "../modules/items_v3"

  environment_name = "stage"
  image_uri        = data.aws_ecr_image.items_v3.image_uri
}
