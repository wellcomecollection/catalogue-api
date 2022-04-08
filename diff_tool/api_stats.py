import httpx


def get_index_name(api_url):
    """
    Returns the name of the index used by this API.
    """
    search_templates_resp = httpx.get(
        f"https://{api_url}/catalogue/v2/_elasticConfig"
    )
    return search_templates_resp.json()["worksIndex"]


def get_api_stats(*, api_url):
    """
    Returns some index stats about the API, including the index name and a breakdown
    of work types in the index.
    """
    index_name = httpx.get(
        f"https://{api_url}/catalogue/v2/_elasticConfig"
    ).json()['worksIndex']

    work_types = httpx.get(
        f"https://{api_url}/catalogue/v2/management/_workTypes"
    ).json()

    return {"index_name": index_name, "work_types": work_types}
