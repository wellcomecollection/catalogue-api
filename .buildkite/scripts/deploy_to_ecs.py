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

    service_description = ecs.describe_services(cluster=cluster, services=[service])[
        "services"
    ][0]

    task_definition = ecs.describe_task_definition(
        taskDefinition=service_description["taskDefinition"]
    )["taskDefinition"]

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

    return manifests[0]['imageId']['imageDigest']


def redeploy_ecs_service(sess, *, cluster, service):
    """
    Force a new deployment of an ECS service.
    """
    ecs = sess.client("ecs")

    resp = ecs.update_service(cluster=cluster, service=service, forceNewDeployment=True)

    return resp["service"]["deployments"][0]["id"]


def describe_tasks_in_service(sess, *, cluster, service):
    """
    Given the name of a service, return a list of tasks running within
    the service.
    """
    ecs_client = sess.client("ecs")

    task_arns = []

    paginator = ecs_client.get_paginator("list_tasks")
    for page in paginator.paginate(cluster=cluster, serviceName=service):
        task_arns.extend(page["taskArns"])

    # If task_arns is empty we can't ask to describe them.
    # TODO: This method can handle up to 100 task ARNs.  It seems unlikely
    # we'd ever have more than that, hence not handling it properly.
    if task_arns:
        resp = ecs_client.describe_tasks(
            cluster=cluster,
            tasks=task_arns,
            include=["TAGS"]
        )

        return resp["tasks"]
    else:
        return []


def get_desired_task_count(sess, *, cluster, service):
    ecs = sess.client("ecs")

    resp = ecs.describe_services(cluster=cluster, services=[service])

    return resp['services'][0]['desiredCount']


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
            running_tasks = describe_tasks_in_service(sess, cluster=self.cluster, service=service)

            for task in running_tasks:
                app_container = next(container for container in task['containers'] if container['name'] == 'app')

                task_id = task['taskArn'].split('/')[-1]

                # If this task is running an image with a different digest,
                # then we know it's not up-to-date yet.
                if app_container['imageDigest'] != ecr_image_digests[app_container['image']]:
                    if task_id not in self.already_checked_tasks:
                        print(f'{service}:\n\ttask {task_id} is running the wrong container')
                        print(f'\texpected: {ecr_image_digests[app_container["image"]]}')
                        print(f'\tactual:   {app_container["imageDigest"]}')
                        self.already_checked_tasks.add(task_id)
                    is_up_to_date = False
                    continue

                if any(container['lastStatus'] != 'RUNNING' for container in task['containers']):
                    print(f'{service}: task {task_id} has containers in the wrong state')
                    is_up_to_date = False
                    continue

            if len(running_tasks) < self.desired_task_counts[service]:
                print(f'{service}: not running enough tasks')
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
    #
    # for service in args['services']:
    #     print(f"*** Forcing a redeployment of {service}")
    #     redeploy_ecs_service(sess, cluster=args['cluster'], service=service)

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
            time.sleep(15)

    else:  # no break
        print("Tasks did not deploy in an hour, failing")
        sys.exit(1)
