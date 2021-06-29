import boto3
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


def get_secret_string(secrets_client, *, secret_id):
    """
    Look up the value of a SecretString in Secrets Manager.
    """
    return secrets_client.get_secret_value(SecretId=secret_id)["SecretString"]


def get_api_es_url(session):
    """
    Returns the Elasticsearch URL for the catalogue cluster.
    """
    secrets = session.client("secretsmanager")

    host = get_secret_string(secrets, secret_id="elasticsearch/catalogue_api/public_host")
    port = get_secret_string(secrets, secret_id="elasticsearch/catalogue_api/port")
    protocol = get_secret_string(secrets, secret_id="elasticsearch/catalogue_api/protocol")
    username = get_secret_string(secrets, secret_id="elasticsearch/catalogue_api/search/username")
    password = get_secret_string(secrets, secret_id="elasticsearch/catalogue_api/search/password")

    return f"{protocol}://{username}:{password}@{host}:{port}"


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
    es_url = get_api_es_url(session)

    index_name = get_index_name(api_url)

    search_resp = httpx.request(
        "GET",
        es_url + f"/{index_name}/_search",
        json={"size": 0, "aggs": {"work_type": {"terms": {"field": "type"}}}},
    )

    work_types = {
        bucket["key"]: bucket["doc_count"]
        for bucket in search_resp.json()["aggregations"]["work_type"]["buckets"]
    }

    work_types["TOTAL"] = sum(work_types.values())

    return {"index_name": index_name, "work_types": work_types}
