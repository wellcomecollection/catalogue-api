# catalogue-api shared infrastructure

This stack contains the catalogue-api elasticsearch cluster configuration, including secrets that services require to run.

## Creating the shared stack from scratch

At present you will need to run a `terraform apply` operation, and then run the scripts in the `./scripts` folder.

Run the scripts in this order:

- [./scripts/create_elastic_users_catalogue.py](./scripts/create_elastic_users_catalogue.py): Creates roles and users for catalogue-api services, along with any other cluster configuration that doesn't make sense to apply in Terraform.
- [./scripts/create_elastic_users_identity.py](./scripts/create_elastic_users_identity.py): Creates the users required in the identity account (specifically at present only the requests API).

## Improvements

We have used a local-exec provisioner in terraform to run these scripts in the past, but adding users safely without rotating existing passwords is tricky. We should find a way to automate this safely from terraform.
