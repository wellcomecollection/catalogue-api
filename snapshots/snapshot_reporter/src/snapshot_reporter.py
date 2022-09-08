"""
This lambda queries Elasticsearch for catalogue snapshots made within the last
day. The index queried is available on the reporting cluster.

This lambda should be triggered by a daily CloudWatch event.
"""

import datetime

import boto3
from elasticsearch import Elasticsearch
import httpx
import humanize
import pytz

from dateutil import parser
import json
import os


def get_secret(session, *, secret_id):
    secrets_client = session.client("secretsmanager")

    resp = secrets_client.get_secret_value(SecretId=secret_id)

    try:
        # The secret response may be a JSON string of the form
        # {"username": "…", "password": "…", "endpoint": "…"}
        secret = json.loads(resp["SecretString"])
    except ValueError:
        secret = resp["SecretString"]

    return secret


def get_elastic_client(session, *, elastic_secret_id):
    secret = get_secret(session, secret_id=elastic_secret_id)

    return Elasticsearch(
        secret["endpoint"], http_auth=(secret["username"], secret["password"])
    )


def get_snapshots(es_client, *, snapshots_index):
    response = es_client.search(
        index=snapshots_index,
        body={
            "query": {
                "bool": {
                    "filter": [
                        {"range": {"snapshotJob.requestedAt": {"gte": "now-1d/d"}}}
                    ]
                }
            },
            "sort": [{"snapshotJob.requestedAt": {"order": "desc"}}],
        },
    )

    return [hit["_source"] for hit in response["hits"]["hits"]]


def get_catalogue_api_document_count(endpoint):
    """
    How many documents are available in the catalogue API?
    """
    resp = httpx.get(f"https://api.wellcomecollection.org/catalogue/v2/{endpoint}")
    return resp.json()["totalResults"]


def format_date(d):
    # The timestamps passed around by the snapshots pipeline are all UTC.
    # This Lambda reports into our Slack channel, so adjust the time if
    # necessary for the UK, and add the appropriate timezone label.
    d = d.astimezone(pytz.timezone("Europe/London"))

    if d.date() == datetime.datetime.now().date():
        return d.strftime("today at %-I:%M %p %Z")
    elif d.date() == (datetime.datetime.now() - datetime.timedelta(days=1)).date():
        return d.strftime("yesterday at %-I:%M %p %Z")
    else:
        return d.strftime("on %A, %B %-d at %-I:%M %p %Z")


def prepare_slack_payload(*, snapshots, api_document_count, recent_updates):
    def _snapshot_message(snapshot):
        index_name = snapshot["snapshotResult"]["indexName"]
        snapshot_document_count = snapshot["snapshotResult"]["documentCount"]

        requested_at = parser.parse(snapshot["snapshotJob"]["requestedAt"])

        # In general, a snapshot should have the same number of works as the
        # catalogue API.  There might be some drift, if new works appear in the
        # pipeline between the snapshot being taken and the reporter running,
        # but not much.  The threshold 25 is chosen somewhat arbitrarily.
        if api_document_count == snapshot_document_count:
            api_comparison = "same as the catalogue API"
        elif abs(api_document_count - snapshot_document_count) < 25:
            api_comparison = "almost the same as the catalogue API"
        else:
            api_comparison = f"*different from the catalogue API, which has {humanize.intcomma(api_document_count)}*"

        return "\n".join(
            [
                f"The latest snapshot is of index *{index_name}*, taken *{format_date(requested_at)}*.",
                f"• It contains {humanize.intcomma(snapshot_document_count)} documents ({api_comparison})",
            ]
        )

    if snapshots:
        latest_snapshot = snapshots[0]

        snapshot_heading = ":white_check_mark: Catalogue Snapshot"
        snapshot_message = _snapshot_message(latest_snapshot)
    else:
        kibana_logs_link = "https://logging.wellcomecollection.org/goto/c98eb0e4e37c802e60d5affea422a98e"
        snapshot_heading = ":interrobang: Catalogue Snapshot not found"
        snapshot_message = (
            f"No snapshot found within the last day. See logs: {kibana_logs_link}"
        )

    if recent_updates["count"]:
        snapshot_message += f"\n• There {'have' if recent_updates['count'] > 1 else 'has'} been {humanize.intcomma(recent_updates['count'])} update{'s' if recent_updates['count'] > 1 else ''} in the last {recent_updates['hours']} hours."
        update_blocks = []
    else:
        delta = datetime.datetime.now(datetime.timezone.utc) - recent_updates["latest"]
        message = f":warning: There haven't been any updates in the last {recent_updates['hours']} hours. The last update was at {format_date(recent_updates['latest'])} ({humanize.naturaldelta(delta)} ago)."
        update_blocks = [
            {"type": "section", "text": {"type": "mrkdwn", "text": message}}
        ]

    snapshot_blocks = [
        {"type": "header", "text": {"type": "plain_text", "text": snapshot_heading}},
        {"type": "section", "text": {"type": "mrkdwn", "text": snapshot_message}},
    ]

    return {"blocks": snapshot_blocks + update_blocks}


def post_to_slack(session, *, slack_secret_id, payload):
    slack_endpoint = get_secret(session, secret_id=slack_secret_id)
    resp = httpx.post(slack_endpoint, json=payload)

    print(f"Sent payload to Slack: {resp}")

    if resp.status_code != 200:
        print("Non-200 response from Slack:")

        print("")

        print("== request ==")
        print(json.dumps(payload, indent=2, sort_keys=True))

        print("")

        print("== response ==")
        print(resp.text)


def get_recent_update_stats(session, *, hours):
    works_index_name = httpx.get(
        "https://api.wellcomecollection.org/catalogue/v2/_elasticConfig"
    ).json()["worksIndex"]

    index_date = works_index_name.replace("works-indexed-", "")
    secret_prefix = f"elasticsearch/pipeline_storage_{index_date}"

    username = get_secret(
        session, secret_id=f"{secret_prefix}/snapshot_generator/es_username"
    )
    password = get_secret(
        session, secret_id=f"{secret_prefix}/snapshot_generator/es_password"
    )
    host = get_secret(session, secret_id=f"{secret_prefix}/public_host")

    pipeline_es_client = Elasticsearch(
        f"https://{host}:9243", http_auth=(username, password)
    )

    indexed_after = datetime.datetime.now() - datetime.timedelta(hours=hours)

    count_resp = pipeline_es_client.count(
        index=works_index_name,
        body={
            "query": {
                "bool": {
                    "filter": [
                        {
                            "range": {
                                "state.indexedTime": {
                                    "gte": indexed_after.strftime("%Y-%m-%dT%H:%M:%SZ")
                                }
                            }
                        }
                    ]
                }
            }
        },
    )

    search_resp = pipeline_es_client.search(
        index=works_index_name,
        body={
            "sort": [{"state.indexedTime": {"order": "desc"}}],
            "_source": ["state.indexedTime"],
            "size": 1,
        },
    )

    return {
        "hours": hours,
        "count": count_resp["count"],
        "latest": parser.parse(
            search_resp["hits"]["hits"][0]["_source"]["state"]["indexedTime"]
        ),
    }


def main(*args):
    session = boto3.Session()

    elastic_secret_id = os.environ["ELASTIC_SECRET_ID"]
    slack_secret_id = os.environ["SLACK_SECRET_ID"]
    snapshots_index = os.environ["ELASTIC_INDEX"]

    elastic_client = get_elastic_client(session, elastic_secret_id=elastic_secret_id)

    snapshots = get_snapshots(elastic_client, snapshots_index=snapshots_index)
    api_document_count = get_catalogue_api_document_count(endpoint="works")

    hours = 24

    recent_updates = get_recent_update_stats(session, hours=hours)

    slack_payload = prepare_slack_payload(
        snapshots=snapshots,
        api_document_count=api_document_count,
        recent_updates=recent_updates,
    )

    post_to_slack(session, slack_secret_id=slack_secret_id, payload=slack_payload)


if __name__ == "__main__":
    main()
