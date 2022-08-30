#!/usr/bin/env python3
"""
Given a set of ECS services, this script will ensure that every task
is using the most up-to-date version of the container image in the
task definition.

## Scenarios

1.  There are no tasks running.  We're trivially done, so stop.

2.  There are three tasks running, with the task definition `ecr/app:latest`.
    If we look up that tag in ECR, that has the image digest `sha256:123`.

    All three tasks are running an image with that image digest, so we're done.

3.  There are three tasks running, with the task definition `ecr/app:latest`.
    If we look up that tag in ECR, that has the image digest `sha256:123`.
    If we look at the tasks, they're using images with digest `sha256:456`.

    Trigger a rolling deployment until all the tasks are using the new digest.

    Note: this will wait forever for a deployment to complete; you should
    configure a timeout in Buildkite or CI if you want to be notified of
    the changes failing to deploy.

"""

import argparse
import collections
import datetime
import functools
import itertools
import json
import re
import sys
import time

from aws_utils import get_aws_session


def parse_args():
    parser = argparse.ArgumentParser(
        description="Ensure ECS tasks are running up-to-date images."
    )
    parser.add_argument("--cluster", help="ECS cluster name", required=True)
    parser.add_argument(
        "--services",
        help="A comma-separated list of service names (e.g. `matcher,merger`)",
        required=True,
        metavar="SERV1,SERV2,SERV3,...",
    )
    parser.add_argument(
        "--role-arn", help="An AWS role to assume when performing ECS operations."
    )

    args = parser.parse_args()

    return {
        "cluster": args.cluster,
        "services": [serv.strip() for serv in args.services.split(",")],
        "role_arn": args.role_arn,
    }


def chunked_iterable(iterable, *, size):
    # See https://alexwlchan.net/2018/12/iterating-in-fixed-size-chunks/
    it = iter(iterable)
    while True:
        chunk = tuple(itertools.islice(it, size))
        if not chunk:
            break
        yield chunk


@functools.lru_cache
def get_ecr_image_digest(sess, *, image_uri):
    ecr = sess.client("ecr")

    # e.g. 760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome/nginx_apigw:f1188c2a7df01663dd96c99b26666085a4192167
    m = re.match(
        r"^(?P<registry_id>[0-9]+)"
        r"\.dkr\.ecr\.eu-west-1\.amazonaws.com/"
        r"(?P<repository_name>[^:]+)"
        r":"
        r"(?P<image_tag>[a-z0-9\.-_]+)",
        image_uri,
    )

    if m is None:
        raise ValueError(f"Could not parse ECR image URI: {image_uri}")

    resp = ecr.describe_images(
        registryId=m.group("registry_id"),
        repositoryName=m.group("repository_name"),
        imageIds=[{"imageTag": m.group("image_tag")}],
    )

    assert (
        len(resp["imageDetails"]) == 1
    ), f"Looked up non-existent ECR image {image_uri}"

    return resp["imageDetails"][0]["imageDigest"]


def get_expected_container_definitions(sess, *, cluster, service_name):
    """
    Given an ECS service, look up the container definition of the current
    task definition and the image digest of the current image in ECR.

    The result is an object like:

        {
            "task_definition": "arn:aws:ecs:eu-west-1:756629837203:task-definition/matcher:4",
            "containers": {
                "app": {
                    "uri": "ecr.public.123/matcher:env.prod",
                    "digest": "sha256:1234567890"
                },
                "logging": {
                    "uri": "ecr.public.123/logging:env.prod",
                    "digest": "sha256:0987654321"
                }
            }
        }

    """
    ecs = sess.client("ecs")

    # First we get the task definition ARN for this ECS service
    service_resp = ecs.describe_services(cluster=cluster, services=[service_name])
    assert len(service_resp["services"]) == 1, "Tried to look up non-existent service"

    task_definition_arn = service_resp["services"][0]["taskDefinition"]

    # Then we look up the container URIs and corresponding image digest
    # specified in this task definition
    task_resp = ecs.describe_task_definition(taskDefinition=task_definition_arn)
    containers = {
        c["name"]: {
            "uri": c["image"],
            "digest": get_ecr_image_digest(sess, image_uri=c["image"]),
        }
        for c in task_resp["taskDefinition"]["containerDefinitions"]
    }

    return {"task_definition": task_definition_arn, "containers": containers}


def check_if_tasks_match_expected_containers(sess, *, cluster, expected_containers):
    """
    Checks to see if all the running tasks match the expected containers.

    It returns a dict (service) -> (issues), e.g.

        {
            "matcher": [
                "Task ABC: container 'app' is running an image with the wrong digest",
                "Task XYZ: container 'app' is running an image with the wrong digest",
            ]
        }

    If the dict is empty, there's no mismatch -- all services are up-to-date.

    """
    # First get a list of all services running in the cluster.  This is
    # slightly more efficient than getting the tasks on a service-by-service
    # basis, because most services will be running <10 tasks (the most we
    # could get from a single ListTasks call).
    ecs = sess.client("ecs")
    paginator = ecs.get_paginator("list_tasks")

    task_arns = []

    for page in paginator.paginate(cluster=cluster):
        task_arns.extend(page["taskArns"])

    # Now we go through and get the more detailed information for each task.
    # We can fetch up to 100 tasks at a time.
    tasks = []
    for batch in chunked_iterable(task_arns, size=100):
        resp = ecs.describe_tasks(cluster=cluster, tasks=batch)
        tasks.extend(resp["tasks"])

    # Finally, we go through and inspect all the tasks and see if they're
    # up-to-date.
    result = collections.defaultdict(list)

    for t in tasks:
        task_id = t["taskArn"].split("/")[-1]

        # We don't care about stopped tasks
        if t["lastStatus"] == "STOPPED":
            continue

        # e.g. 'group': 'service:prod-search-api',
        service = t["group"].replace("service:", "")

        # Do we care about this task?  If it's a service we're not looking
        # at, we can skip it.
        try:
            expected_task_containers = expected_containers[service]["containers"]
        except KeyError:
            continue

        actual_task_containers = {
            c['name']: {
                'uri': c['image'],
                'digest': c['imageDigest']
            }
            for c in t['containers']
        }

        if actual_task_containers != expected_task_containers:
            result[service].append(
                f'Incorrect containers in task {task_id}.\n'
                f'Expected:\n' +
                json.dumps(expected_task_containers, indent=2, sort_keys=True) + '\n' +
                f'Actual:\n' +
                json.dumps(actual_task_containers, indent=2, sort_keys=True)
            )

    return result


if __name__ == "__main__":
    args = parse_args()

    sess = get_aws_session(role_arn=args["role_arn"])

    expected_containers = {
        service_name: get_expected_container_definitions(
            sess, cluster=args["cluster"], service_name=service_name
        )
        for service_name in args["services"]
    }

    print("Got expected container definitions and digests:")
    print(json.dumps(expected_containers, indent=2, sort_keys=True))
    print("")

    result = check_if_tasks_match_expected_containers(
        sess, cluster=args["cluster"], expected_containers=expected_containers
    )
    if result == {}:
        print('All tasks are up-to-date, nothing to do 🎉')
        sys.exit(0)
    else:
        print('Tasks are not up-to-date, triggering a deployment')
        ecs = sess.client('ecs')
        for stale_service in result:
            ecs_client.update_service(
                    cluster=cluster,
                    service=stale_service,
                    forceNewDeployment=True
                )

    while True:
        now = datetime.datetime.now().isoformat()
        print(f"Checking if tasks match expected containers at {now}")

        result = check_if_tasks_match_expected_containers(
            sess, cluster=args["cluster"], expected_containers=expected_containers
        )
        if result == {}:
            print('All tasks are up-to-date, all done 🎉')
            sys.exit(0)

        print(result)
        # time.sleep(10)
        break
