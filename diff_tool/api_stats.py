import boto3
from elasticsearch import Elasticsearch
import httpx


def get_session_with_role(role_arn, region_name="eu-west-1"):
    """
    Returns a boto3.Session that uses the given role ARN.
    """
    sts_client = boto3.client("sts")

    assumed_role_object = sts_client.assume_role(
        RoleArn=role_arn, RoleSessionName="AssumeRoleSession1"
    )
    credentials = assumed_role_object["Credentials"]
    return boto3.Session(
        aws_access_key_id=credentials["AccessKeyId"],
        aws_secret_access_key=credentials["SecretAccessKey"],
        aws_session_token=credentials["SessionToken"],
        region_name=region_name,
    )


def get_secret_string(session, *, secret_id):
    """
    Look up the value of a SecretString in Secrets Manager.
    """
    secrets_client = session.client("secretsmanager")
    return secrets_client.get_secret_value(SecretId=secret_id)["SecretString"]


def get_pipeline_storage_es_client(session, *, pipeline_date):
    """
    Returns an Elasticsearch client for the pipeline-storage cluster.
    """
    secret_prefix = f"elasticsearch/pipeline_storage_{pipeline_date}"

    host = get_secret_string(session, secret_id=f"{secret_prefix}/public_host")
    port = get_secret_string(session, secret_id=f"{secret_prefix}/port")
    protocol = get_secret_string(session, secret_id=f"{secret_prefix}/protocol")
    username = get_secret_string(
        session, secret_id=f"{secret_prefix}/read_only/es_username"
    )
    password = get_secret_string(
        session, secret_id=f"{secret_prefix}/read_only/es_password"
    )
    return Elasticsearch(f"{protocol}://{username}:{password}@{host}:{port}")


def get_index_name(api_url):
    """
    Returns the name of the index used by this API.
    """
    search_templates_resp = httpx.get(
        f"https://{api_url}/catalogue/v2/search-templates.json"
    )
    return search_templates_resp.json()["templates"][0]["index"]


def get_api_stats(session, *, api_url):
    """
    Returns some index stats about the API, including the index name and a breakdown
    of work types in the index.
    """
    index_name = get_index_name(api_url)
    pipeline_date = index_name.replace("works-indexed-", "")

    es_client = get_pipeline_storage_es_client(session, pipeline_date=pipeline_date)

    search_resp = es_client.search(
        index=index_name,
        body={"size": 0, "aggs": {"work_type": {"terms": {"field": "type"}}}},
    )

    aggregations = search_resp["aggregations"]

    work_types = {
        bucket["key"]: bucket["doc_count"]
        for bucket in aggregations["work_type"]["buckets"]
    }

    work_types["TOTAL"] = sum(work_types.values())

    return {"index_name": index_name, "work_types": work_types}
