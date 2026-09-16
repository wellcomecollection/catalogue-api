output "hostnames" {
  value = {
    prod  = module.identifiers_prod.hostname
    stage = module.identifiers_stage.hostname
  }
}

output "invoke_urls" {
  value = {
    prod  = module.identifiers_prod.invoke_url
    stage = module.identifiers_stage.invoke_url
  }
}

output "lambda_names" {
  value = {
    prod  = module.identifiers_prod.lambda_name
    stage = module.identifiers_stage.lambda_name
  }
}
