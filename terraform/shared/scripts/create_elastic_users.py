#!/usr/bin/env python
"""
This script is run by a Terraform local-exec provisioner to create roles/users in
an Elastic Cloud cluster immediately after it's been created.
"""

import functools
import secrets

import boto3
import click

from elasticsearch import Elasticsearch

WORK_INDEX_PATTERN = "works-{index}*"
IMAGE_INDEX_PATTERN = "images-{index}*"

ROLES = {
    "catalogue_read": {
        "indices": [
            {
                "names": [WORK_INDEX_PATTERN, IMAGE_INDEX_PATTERN],
                "privileges": ["read", "view_index_metadata"]
            }
        ]
    },
    "catalogue_manage_ccr": {
        "indices": [
            {
                "names": [WORK_INDEX_PATTERN, IMAGE_INDEX_PATTERN],
                "privileges": ["manage_follow_index"]
            }
        ],
        "cluster": ["manage_ccr"]
    }
}

SERVICES = {
    "catalogue_api": ["catalogue_read"],
    "diff_tool": ["catalogue_read"],
    "replication_manager": ["catalogue_read", "catalogue_manage_ccr"]
}


@functools.lru_cache()
def get_aws_client(resource, *, role_arn):
    """
    Get a boto3 client authenticated against the given role.
    """
    sts_client = boto3.client("sts")
    assumed_role_object = sts_client.assume_role(
        RoleArn=role_arn, RoleSessionName="AssumeRoleSession1"
    )

    credentials = assumed_role_object["Credentials"]

    return boto3.client(
        resource,
        aws_access_key_id=credentials["AccessKeyId"],
        aws_secret_access_key=credentials["SecretAccessKey"],
        aws_session_token=credentials["SessionToken"],
    )


def read_secret(secret_id, role_arn):
    """
    Retrieve a secret from Secrets Manager.
    """
    secrets_client = get_aws_client("secretsmanager", role_arn=role_arn)

    return secrets_client.get_secret_value(SecretId=secret_id)["SecretString"]


def store_secret(secret_id, secret_value, role_arn):
    """
    Store a key/value pair in Secrets Manager.
    """

    secrets_client = get_aws_client("secretsmanager", role_arn=role_arn)

    resp = secrets_client.put_secret_value(
        SecretId=secret_id, SecretString=secret_value
    )

    if resp["ResponseMetadata"]["HTTPStatusCode"] != 200:
        raise RuntimeError(f"Unexpected error from PutSecretValue: {resp}")

    click.echo(f"Stored secret {click.style(secret_id, 'yellow')}")


if __name__ == '__main__':
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
    secret_prefix = f"elasticsearch/catalogue_api"

    es_host = read_secret(f"{secret_prefix}/public_host", role_arn)
    es_protocol = read_secret(f"{secret_prefix}/protocol", role_arn)
    es_port = read_secret(f"{secret_prefix}/port", role_arn)

    username = read_secret(f"{secret_prefix}/username", role_arn)
    password = read_secret(f"{secret_prefix}/password", role_arn)

    endpoint = f"https://{es_host}:{es_port}"

    es = Elasticsearch(endpoint, http_auth=(username, password))

    # Create roles
    for role_name, index_privileges in ROLES.items():
        es.security.put_role(
            role_name,
            body=index_privileges,
        )

    # Create usernames
    newly_created_usernames = []
    for service_name, roles in SERVICES.items():
        service_password = secrets.token_hex()
        es.security.put_user(
            username=service_name,
            body={
                "password": service_password,
                "roles": roles
            }
        )

        newly_created_usernames.append((service_name, service_password))

    # Store secrets
    for service_name, service_password in newly_created_usernames:
        store_secret(
            secret_id=f"{secret_prefix}/{service_name}/username",
            secret_value=service_name,
            role_arn=role_arn
        )

        store_secret(
            secret_id=f"{secret_prefix}/{service_name}/password",
            secret_value=service_password,
            role_arn=role_arn
        )
