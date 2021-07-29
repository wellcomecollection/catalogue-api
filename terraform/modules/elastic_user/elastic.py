import functools
import boto3
from elasticsearch import Elasticsearch


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


def get_es_client():
    """
    Gets a catalogue-api elasticsearch client
    """
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
    secret_prefix = f"elasticsearch/catalogue_api"

    es_host = read_secret(f"{secret_prefix}/public_host", role_arn)
    es_port = read_secret(f"{secret_prefix}/port", role_arn)

    username = read_secret(f"{secret_prefix}/username", role_arn)
    password = read_secret(f"{secret_prefix}/password", role_arn)
    endpoint = f"https://{es_host}:{es_port}"

    return Elasticsearch(endpoint, http_auth=(username, password))
