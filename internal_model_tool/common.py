import boto3
import base64
import json
import re
from urllib.request import Request, urlopen


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


def get_local_date():
    config_file = open(
        "../common/display/src/main/scala/weco/catalogue/display_model/ElasticConfig.scala",
        "r",
    )
    config_text = config_file.read()
    config_file.close()
    date = re.findall('val indexDate = "(.*)"', config_text)[0]
    return date


def get_remote_latest_internal_model(session, date):
    # this comes in the format of
    # {"model.versions.4988":"884bf55a45d76171db88112142b2ee8dd6e37f46","model.versions.4960":"55842c7dffef3b80fe7755768728028d75f0d866"}
    # So we choose the highest available version
    meta = get_remote_meta(session, date)
    version = max([int(key.split(".")[-1]) for key in meta])
    hash = meta.get(f"model.versions.{version}")
    return f"{version}.{hash}"


def get_secret_string(session, *, secret_id):
    secrets_client = session.client("secretsmanager")
    resp = secrets_client.get_secret_value(SecretId=secret_id)
    return resp["SecretString"]


def get_remote_meta(session, date):
    host = get_secret_string(session, secret_id="elasticsearch/catalogue_api/public_host")
    username = get_secret_string(session, secret_id="elasticsearch/catalogue_api/internal_model_tool/username")
    password = get_secret_string(session, secret_id="elasticsearch/catalogue_api/internal_model_tool/password")

    index = f"works-indexed-{date}"
    auth = base64.b64encode(f"{username}:{password}".encode()).decode("utf-8")
    request = Request(f"https://{host}:9243/{index}/_mapping")
    request.add_header("Authorization", f"Basic {auth}")
    meta = json.loads(urlopen(request).read())[f"works-indexed-{date}"]["mappings"][
        "_meta"
    ]
    return meta


def get_local_internal_model():
    deps_file = open("../project/Dependencies.scala", "r")
    config_text = deps_file.read()
    deps_file.close()
    model_version = re.findall('val internalModel = "(.*)"', config_text)[0]
    return model_version


def set_local_internal_model(latest_version):
    old_lines = list(open("../project/Dependencies.scala"))

    with open("../project/Dependencies.scala", "w") as out_file:
        for line in old_lines:
            if line.startswith("    val internalModel"):
                out_file.write(f'    val internalModel = "{latest_version}"\n')
            else:
                out_file.write(line)
