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


def prepare_slack_payload(*, snapshots, api_counts, recent_updates):
    def _snapshot_message(title, snapshot, doc_updates, api_document_count):
        if not snapshot:
            kibana_logs_link = "https://logging.wellcomecollection.org/app/r/s/qYYcM"
            return [
                {
                    "type": "header",
                    "text": {
                        "type": "plain_text",
                        "emoji": True,
                        "text": f":rotating_light: {title}",
                    },
                },
                {
                    "type": "section",
                    "text": {
                        "type": "mrkdwn",
                        "text": f"_*No snapshot found within the last {recent_updates['hours']} hours*_. See logs: {kibana_logs_link}",
                    },
                },
            ]

        warnings = False
        index_name = snapshot["snapshotResult"]["indexName"]
        snapshot_document_count = snapshot["snapshotResult"]["documentCount"]
        started_at = parser.parse(snapshot["snapshotResult"]["startedAt"])
        finished_at = parser.parse(snapshot["snapshotResult"]["finishedAt"])

        last_update_delta = humanize.naturaldelta(
            datetime.datetime.now(datetime.timezone.utc) - doc_updates["last_update"]
        )
        last_update = format_date(doc_updates["last_update"])

        # In general, a snapshot should have the same number of works as the
        # catalogue API.  There might be some drift, if new works appear in the
        # pipeline between the snapshot being taken and the reporter running,
        # but not much.  The threshold 25 is chosen somewhat arbitrarily.
        if api_document_count == snapshot_document_count:
            api_comparison = "the same as the catalogue API"
        elif abs(api_document_count - snapshot_document_count) < 25:
            api_comparison = "almost the same as the catalogue API"
        else:
            warnings = True
            api_comparison = f":warning: *different from the catalogue API, which has {humanize.intcomma(api_document_count)}*"

        if doc_updates["count"]:
            plural = doc_updates["count"] > 1
            humanized_count = humanize.intcomma(doc_updates["count"])
            updates_message = f"There {'have' if plural else 'has'} been *{humanized_count}* update{'s' if plural else ''} in the last {recent_updates['hours']} hours."
        else:
            warnings = True
            updates_message = f":warning: _*There haven't been any updates in the last {recent_updates['hours']} hours*_."

        updates_message += f" The last update was {last_update} ({last_update_delta} ago)."

        return [
            {
                "type": "header",
                "text": {
                    "type": "plain_text",
                    "emoji": True,
                    "text": f"{':warning:' if warnings else ':white_check_mark:'} {title}",
                },
            },
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": "\n".join(
                        [
                            f"- Index *{index_name}*",
                            f"- Snapshot taken *{format_date(started_at)}*",
                            f"- It contains *{humanize.intcomma(snapshot_document_count)}* documents (_{api_comparison}_).",
                            f"- Snapshot took *{humanize.naturaldelta(finished_at - started_at)}* to complete.",
                            f"- {updates_message}",
                        ]
                    ),
                },
            },
        ]

    works_index = recent_updates["works"]["index"]
    images_index = recent_updates["images"]["index"]
    works_snapshot = next(
        (s for s in snapshots if s["snapshotResult"]["indexName"] == works_index), None
    )
    images_snapshot = next(
        (s for s in snapshots if s["snapshotResult"]["indexName"] == images_index), None
    )

    works_message = _snapshot_message(
        title="Works :scroll:",
        snapshot=works_snapshot,
        doc_updates=recent_updates["works"],
        api_document_count=api_counts["works"],
    )
    images_message = _snapshot_message(
        title="Images :frame_with_picture:",
        snapshot=images_snapshot,
        doc_updates=recent_updates["images"],
        api_document_count=api_counts["images"],
    )
    header = {
        "type": "header",
        "text": {
            "type": "plain_text",
            "emoji": True,
            "text": ":camera_with_flash: Catalogue snapshots",
        },
    }

    return {"blocks": [header] + works_message + images_message}


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
    indices = httpx.get(
        "https://api.wellcomecollection.org/catalogue/v2/_elasticConfig"
    ).json()
    works_index_name = indices["worksIndex"]
    images_index_name = indices["imagesIndex"]

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
    indexed_time_body = {
        "query": {
            "bool": {
                "filter": [
                    {
                        "range": {
                            "debug.indexedTime": {
                                "gte": indexed_after.strftime("%Y-%m-%dT%H:%M:%SZ")
                            }
                        }
                    }
                ]
            }
        },
    }
    last_update_body = {
        "size": 1,
        "_source": ["debug.indexedTime"],
        "sort": [{"debug.indexedTime": {"order": "desc"}}]
    }
    works_count = pipeline_es_client.count(
        index=works_index_name, body=indexed_time_body
    )["count"]
    images_count = pipeline_es_client.count(
        index=images_index_name, body=indexed_time_body
    )["count"]

    works_last_update = pipeline_es_client.search(
        index=works_index_name, body=last_update_body
    )["hits"]["hits"][0]["_source"]["debug"]["indexedTime"]
    images_last_update = pipeline_es_client.search(
        index=images_index_name, body=last_update_body
    )["hits"]["hits"][0]["_source"]["debug"]["indexedTime"]

    return {
        "hours": hours,
        "works": {
            "index": works_index_name,
            "count": works_count,
            "last_update": parser.parse(works_last_update),
        },
        "images": {
            "index": images_index_name,
            "count": images_count,
            "last_update": parser.parse(images_last_update),
        },
    }


def main(*args):
    session = boto3.Session()

    elastic_secret_id = os.environ["ELASTIC_SECRET_ID"]
    slack_secret_id = os.environ["SLACK_SECRET_ID"]
    snapshots_index = os.environ["ELASTIC_INDEX"]

    elastic_client = get_elastic_client(session, elastic_secret_id=elastic_secret_id)

    snapshots = get_snapshots(elastic_client, snapshots_index=snapshots_index)
    api_counts = {
        "works": get_catalogue_api_document_count(endpoint="works"),
        "images": get_catalogue_api_document_count(endpoint="images"),
    }
    hours = 24

    recent_updates = get_recent_update_stats(session, hours=hours)

    slack_payload = prepare_slack_payload(
        snapshots=snapshots, api_counts=api_counts, recent_updates=recent_updates
    )

    post_to_slack(session, slack_secret_id=slack_secret_id, payload=slack_payload)


if __name__ == "__main__":
    main()
