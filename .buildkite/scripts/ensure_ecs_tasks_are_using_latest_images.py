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

"""

import argparse


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
    parser.add_argument(
        "--timeout",
        help="How many minutes to wait before failing the deployment",
        type=int,
        default=15,
    )

    args = parser.parse_args()

    return {
        "cluster": args.cluster,
        "services": [serv.strip() for serv in args.services.split(",")],
        "role_arn": args.role_arn,
        "timeout": args.timeout,
    }


if __name__ == "__main__":
    args = parse_args()

    print(args)
