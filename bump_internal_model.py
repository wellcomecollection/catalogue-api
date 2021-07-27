#!/usr/bin/env python
"""
This script updates the version of internal_model in Dependencies.scala
to the latest version in S3.

This saves somebody having to look up what the exact version/commit ID is.
"""

import boto3
import re
from urllib.request import Request, urlopen
import json
import base64

def get_session(*, role_arn):
    sts_client = boto3.client("sts")
    assumed_role_object = sts_client.assume_role(
        RoleArn=role_arn, RoleSessionName="AssumeRoleSession1"
    )
    credentials = assumed_role_object["Credentials"]
    return boto3.Session(
        aws_access_key_id=credentials["AccessKeyId"],
        aws_secret_access_key=credentials["SecretAccessKey"],
        aws_session_token=credentials["SessionToken"],
    )


def get_date_from_elastic_config():
    config_file = open("common/display/src/main/scala/weco/catalogue/display_model/ElasticConfig.scala", 'r')
    config_text = config_file.read()
    config_file.close()
    date = re.findall("val indexDate = \"(.*)\"", config_text)[0]
    return date


def get_version_from_es_pipeline(session, date):
    sm = session.client('secretsmanager')
    host = sm.get_secret_value(SecretId='elasticsearch/catalogue_api/public_host')['SecretString']
    username = sm.get_secret_value(SecretId='elasticsearch/catalogue_api/search/username')['SecretString']
    password = sm.get_secret_value(SecretId='elasticsearch/catalogue_api/search/password')['SecretString']

    index = f"works-indexed-{date}"
    auth = base64.b64encode(f"{username}:{password}".encode()).decode("utf-8")
    request = Request(f"https://{host}:9243/{index}/_mapping")
    request.add_header("Authorization", f"Basic {auth}")
    meta = json.loads(urlopen(request).read())[f"works-indexed-{date}"]["mappings"]["_meta"]
    # this comes in the format of {"model.versions.4988":"884bf55a45d76171db88112142b2ee8dd6e37f46","model.versions.4960":"55842c7dffef3b80fe7755768728028d75f0d866"}
    # So we choose the highest available version
    version = max([int(key.split(".")[-1]) for key in meta])
    hash = meta.get(f"model.versions.{version}")
    return f"{version}.{hash}"


def set_internal_model_version(latest_version):
    old_lines = list(open("project/Dependencies.scala"))

    with open("project/Dependencies.scala", "w") as out_file:
        for line in old_lines:
            if line.startswith("    val internalModel"):
                out_file.write(f'    val internalModel = "{latest_version}"\n')
            else:
                out_file.write(line)


if __name__ == "__main__":
    session = get_session(role_arn="arn:aws:iam::760097843905:role/platform-read_only")
    catalogue_session = get_session(role_arn="arn:aws:iam::756629837203:role/catalogue-developer")
    date = get_date_from_elastic_config()
    version = get_version_from_es_pipeline(catalogue_session, date)
    set_internal_model_version(version)

