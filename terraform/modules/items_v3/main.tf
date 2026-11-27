module "lambda" {
  source = "github.com/wellcomecollection/terraform-aws-lambda?ref=v1.3.0"

  name         = "items-v3-api-${var.environment_name}"
  description  = "Items API v3 (${var.environment_name})"
  package_type = "Image"
  image_uri    = var.image_uri

  # Above the service's five second request deadline (items_v3/README.md).
  timeout     = 10
  memory_size = 512
}
