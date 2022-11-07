#!/usr/bin/env python3

import argparse
import sys
import time

import boto3
from botocore.exceptions import ClientError

from ecs import (
    describe_deployment,
    describe_service,
    describe_task_definition,
    redeploy_ecs_service,
)


OKBLUE = "\033[94m"
OKGREEN = "\033[92m"
WARNING = "\033[93m"
FAIL = "\033[91m"
RESET = "\033[0m"


def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument("cluster", metavar="CLUSTER", help="Name of the ECS cluster")
    parser.add_argument(
        "services", metavar="SERVICES", help="Comma-separated list of services"
    )

    args = parser.parse_args()

    return {"cluster": args.cluster, "services": args.services.split(",")}


def get_app_ecr_image_uri(sess, *, cluster, service):
    service = describe_service(sess, cluster=cluster, service=service)
    task_definition = describe_task_definition(
        sess, task_definition_arn=service["taskDefinition"]
    )

    return next(
        container["image"]
        for container in task_definition["containerDefinitions"]
        if container["name"] == "app"
    )


def get_all_ecr_image_uris(sess, *, cluster, services):
    """
    Returns a list of all the image repositories that will be affected
    by this change.
    """
    return {
        get_app_ecr_image_uri(sess, cluster=cluster, service=service)
        for service in services
    }


def retag_ecr_image(sess, *, repository_name, old_tag, new_tag):
    """
    Given an ECR repository, tag the image `old_tag` as `new_tag`.
    """
    ecr = sess.client("ecr")

    manifests = ecr.batch_get_image(
        repositoryName=repository_name, imageIds=[{"imageTag": old_tag}]
    )["images"]

    if len(manifests) == 0:
        raise RuntimeError(f"No matching images found for {repository_name}:{old_tag}!")

    if len(manifests) > 1:
        raise RuntimeError(
            f"Multiple matching images found for {repository_name}:{old_tag}!"
        )

    old_image = manifests[0]

    try:
        ecr.put_image(
            repositoryName=repository_name,
            imageTag=new_tag,
            imageManifest=old_image["imageManifest"],
        )
    except ClientError as e:
        if e.response["Error"]["Code"] != "ImageAlreadyExistsException":
            raise e

    return old_image["imageId"]["imageDigest"]


def pprint_time(seconds):
    seconds = int(seconds)
    if seconds > 60:
        minutes = seconds // 60
        seconds = seconds % 60
        if seconds > 0:
            return f"{minutes}m {seconds}s"
        else:
            return f"{minutes}m"
    else:
        return f"{seconds}s"


if __name__ == "__main__":
    args = parse_args()

    cluster = args["cluster"]

    sess = boto3.Session()

    ecr_image_uris = get_all_ecr_image_uris(sess, **args)

    ecr_image_digests = {}

    for image_uri in ecr_image_uris:
        repository_uri, new_tag = image_uri.split(":")
        _, repository_name = repository_uri.split("/", 1)

        print(f"Tagging image {OKBLUE}{repository_name}:{new_tag}{RESET} from latest")
        ecr_image_digests[image_uri] = retag_ecr_image(
            sess, repository_name=repository_name, old_tag="latest", new_tag=new_tag
        )

    print("")

    pending_deployments = {}

    for service in args["services"]:
        print(f"Starting a new deployment of {OKBLUE}{service}{RESET}")
        pending_deployments[service] = redeploy_ecs_service(
            sess, cluster=args["cluster"], service=service
        )
        print(
            f"Started new deployment of {OKBLUE}{service}{RESET} with deployment {OKBLUE}{pending_deployments[service]}{RESET}"
        )

    print("")

    print("Waiting for deployments to complete...")

    # Wait for 60 minutes to see if services deployed correctly
    now = time.time()

    while time.time() - now < 60 * 60 and pending_deployments:
        time.sleep(15)
        print("")

        for service, deployment_id in sorted(pending_deployments.items()):
            deployment = describe_deployment(
                sess, cluster=cluster, service=service, deployment_id=deployment_id
            )

            if deployment["rolloutState"] == "COMPLETED":
                print(
                    f"Deployment of {OKBLUE}{service}{RESET} has {OKGREEN}completed{RESET} ({OKBLUE}{deployment_id}{RESET})"
                )
                del pending_deployments[service]
            else:
                print(
                    f"Deployment of {OKBLUE}{service}{RESET} is in state {WARNING}{deployment['rolloutState']}{RESET} "
                    f"(running {OKGREEN}{deployment['runningCount']}{RESET}, pending {WARNING}{deployment['pendingCount']}{RESET})"
                )

        if pending_deployments:
            time_elapsed = time.time() - now
            print(f"Waiting another 15s, waited {pprint_time(time_elapsed)} so far")

    print("")

    if pending_deployments:
        print(f"{FAIL}Not all services were deployed successfully{RESET}")
        sys.exit(1)
    else:
        print(f"{OKGREEN}All services deployed successfully{RESET}")
        sys.exit(0)
