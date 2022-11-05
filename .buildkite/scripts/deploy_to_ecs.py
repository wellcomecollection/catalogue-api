#!/usr/bin/env python3

import argparse
import functools
import json
import os
import subprocess
import sys
import time

import boto3
from botocore.exceptions import ClientError

from ecs import (
    describe_running_tasks_in_service,
    describe_service,
    describe_task_definition,
    get_desired_task_count,
    redeploy_ecs_service,
)


def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument("cluster", metavar="CLUSTER", help="Name of the ECS cluster")
    parser.add_argument(
        "services", metavar="SERVICES", help="Comma-separated list of services"
    )

    args = parser.parse_args()

    return {"cluster": args.cluster, "services": args.services.split(",")}


def get_app_ecr_image_uri(sess, *, cluster, service):
    ecs = sess.client("ecs")

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

    existing_manifest = manifests[0]["imageManifest"]

    tag_operation = {
        "source": f"{repository_name}:{old_tag}",
        "target": f"{repository_name}:{new_tag}",
    }

    try:
        ecr.put_image(
            repositoryName=repository_name,
            imageTag=new_tag,
            imageManifest=existing_manifest,
        )
    except ClientError as e:
        if e.response["Error"]["Code"] != "ImageAlreadyExistsException":
            raise e

    return manifests[0]["imageId"]["imageDigest"]


class TaskChecker:
    def __init__(self, sess, *, cluster, services, ecr_image_digests):
        self.already_checked_tasks = set()
        self.sess = sess
        self.cluster = cluster
        self.services = services
        self.ecr_image_digests = ecr_image_digests

        # How many tasks do we want in each service
        self.desired_task_counts = {
            service: get_desired_task_count(sess, cluster=cluster, service=service)
            for service in services
        }

        # What's the ECR URI associated with the task definition
        # in each service?
        self.ecr_image_uris = {
            service: get_app_ecr_image_uri(sess, cluster=cluster, service=service)
            for service in services
        }

    def has_up_to_date_tasks(self):
        """
        Checks whether all the running tasks in a cluster/service are using
        the correct versions of the images.
        """
        # Now loop through all these services, inspect the running tasks,
        # and check if they match the service spec.
        is_up_to_date = True

        ecs = self.sess.client("ecs")

        for service in self.services:
            running_tasks = describe_running_tasks_in_service(
                sess, cluster=self.cluster, service=service
            )

            for task in running_tasks:
                app_container = next(
                    container
                    for container in task["containers"]
                    if container["name"] == "app"
                )

                task_id = task["taskArn"].split("/")[-1]

                # If this task is running an image with a different digest,
                # then we know it's not up-to-date yet.
                if (
                    app_container["imageDigest"]
                    != ecr_image_digests[app_container["image"]]
                ):
                    if task_id not in self.already_checked_tasks:
                        print(
                            f"{service}:\n\ttask {task_id} is running the wrong container"
                        )
                        print(
                            f'\texpected: {ecr_image_digests[app_container["image"]]}'
                        )
                        print(f'\tactual:   {app_container["imageDigest"]}')
                        self.already_checked_tasks.add(task_id)
                    is_up_to_date = False
                    continue

                if any(
                    container["lastStatus"] != "RUNNING"
                    for container in task["containers"]
                ):
                    print(
                        f"{service}: task {task_id} has containers in the wrong state"
                    )
                    is_up_to_date = False
                    continue

            if len(running_tasks) < self.desired_task_counts[service]:
                print(f"{service}: not running enough tasks")
                is_up_to_date = False

        return is_up_to_date


if __name__ == "__main__":
    args = parse_args()

    sess = boto3.Session()

    ecr_image_uris = get_all_ecr_image_uris(sess, **args)

    ecr_image_digests = {}

    for image_uri in ecr_image_uris:
        repository_uri, new_tag = image_uri.split(":")
        _, repository_name = repository_uri.split("/", 1)

        print(f"*** Tagging image {repository_name}:{new_tag} from latest")
        ecr_image_digests[image_uri] = retag_ecr_image(
            sess, repository_name=repository_name, old_tag="latest", new_tag=new_tag
        )

    for service in args["services"]:
        print(f"*** Forcing a redeployment of {service}")
        redeploy_ecs_service(sess, cluster=args["cluster"], service=service)

    # Wait for 60 minutes to see if services deployed correctly
    now = time.time()
    checker = TaskChecker(sess, **args, ecr_image_digests=ecr_image_digests)

    while time.time() - now < 60 * 60:
        res = checker.has_up_to_date_tasks()
        if res:
            print("Tasks are up-to-date, deployment complete!")
            sys.exit(0)
        else:
            print("Still waiting for deployment to complete, waiting another 15s")
            print("")
            time.sleep(15)

    else:  # no break
        print("Tasks did not deploy in an hour, failing")
        sys.exit(1)
