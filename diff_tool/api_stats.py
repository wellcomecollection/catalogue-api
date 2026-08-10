import httpx


def get_index_name(api_url):
    """
    Returns the name of the index used by this API.
    """
    search_templates_resp = httpx.get(f"https://{api_url}/catalogue/v2/_elasticConfig")
    return search_templates_resp.json()["worksIndex"]


def get_api_stats(*, api_url, elastic_cluster=None):
    """
    Returns some index stats about the API, including the index name and a breakdown
    of work types in the index.
    """
    params = {"elasticCluster": elastic_cluster} if elastic_cluster else {}
    index_name = httpx.get(
        f"https://{api_url}/catalogue/v2/_elasticConfig", params=params
    ).json()["worksIndex"]

    work_types = httpx.get(
        f"https://{api_url}/catalogue/v2/management/_workTypes", params=params
    ).json()

    work_types["TOTAL"] = sum(work_types.values())

    return {"index_name": index_name, "work_types": work_types}
