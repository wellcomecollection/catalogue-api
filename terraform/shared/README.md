# catalogue-api shared infrastructure

This stack contains the catalogue-api elasticsearch cluster configuration, including secrets that services require to run.

## Creating the shared stack from scratch

At present you will need to run a `./run_terraform.sh apply` operation, and then run the scripts in the `./scripts` folder.

Run the scripts in this order:

- [./scripts/create_elastic_roles.py](./scripts/create_elastic_roles.py): Creates roles for catalogue-api services.

## Elastic users

Elastic users are managed in terraform. Each have AWS secrets for their username and password. These are then `PUT`
into the Elastic cluster using the [`local-exec` provisioner](https://www.terraform.io/docs/language/resources/provisioners/local-exec.html)
run in the [`elastic_user` module](../modules/elastic_user).

It would be good to provision these with TF, [but currently there looks like there is little appetite](https://github.com/elastic/terraform-provider-ec/issues/344)
to provide this from Elastic.