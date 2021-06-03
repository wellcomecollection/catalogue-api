#!/usr/bin/env python
"""
This script updates the version of internal_model in Dependencies.scala
to the latest version in S3.

This saves somebody having to look up what the exact version/commit ID is.
"""

import boto3


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


def get_latest_internal_model_version(session):
    s3 = session.client("s3")

    list_resp = s3.list_objects_v2(
        Bucket="releases.mvn-repo.wellcomecollection.org",
        Prefix="uk/ac/wellcome/internal_model_2.12/",
        Delimiter="/"
    )

    # e.g. uk/ac/wellcome/internal_model_2.12/4210.8666eda05db68cc4c8a3f3a9/
    prefixes = [cp["Prefix"] for cp in list_resp["CommonPrefixes"]]

    return prefixes[-1].strip("/").split("/")[-1]


def set_internal_model_version(latest_version):
    old_lines = list(open("project/Dependencies.scala"))

    with open("project/Dependencies.scala", "w") as out_file:
        for line in old_lines:
            if line.startswith("    val internalModel"):
                out_file.write(f'    val internalModel = "{latest_version}"\n')
            else:
                out_file.write(line)


if __name__ == '__main__':
    session = get_session(
        role_arn="arn:aws:iam::760097843905:role/platform-read_only"
    )

    latest_version = get_latest_internal_model_version(session)
    set_internal_model_version(latest_version)
