resource "ec_deployment" "things" {
  name = "catalogue-testing"

  region                 = "eu-west-1"
  version                = "7.13.1"
  deployment_template_id = "aws-io-optimized-v2"

  elasticsearch {
    topology {
      id         = "hot_content"
      size       = "2g"
      zone_count = 1
    }

    remote_cluster {
      deployment_id = local.catalogue_ec_cluster_id
      alias         = local.catalogue_ec_cluster_name
      ref_id        = local.catalogue_ec_cluster_ref_id
    }
  }
}