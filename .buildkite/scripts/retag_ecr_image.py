#!/usr/bin/env python3

import argparse

import boto3
from botocore.exceptions import ClientError


OKBLUE = "\033[94m"
RESET = "\033[0m"


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

    try:
        ecr.put_image(
            repositoryName=repository_name,
            imageTag=new_tag,
            imageManifest=manifests[0]["imageManifest"],
        )
    except ClientError as e:
        if e.response["Error"]["Code"] != "ImageAlreadyExistsException":
            raise e



def parse_args():
    parser = argparse.ArgumentParser()

    parser.add_argument("tag", metavar="TAG")

    parser.add_arggument(
        "repo_names",
        metavar="REPO_NAMES",
        help="Comma-separated list of image repository names",
    )

    args = parser.parse_args()

    return {
        "repo_names": repo_names.image_ids.split(","),
        "old_tag": args.old_tag,
        "new_tag": args.new_tag,
    }


if __name__ == '__main__':
    sess = boto3.Session()
    args = parse_args()

    for name in args.repo_names:
        print(f"Tagging image {OKBLUE}{name}:{new_tag}{RESET} from latest")
        retag_ecr_image(sess, repository_name=name, old_tag="latest", new_tag=args.tag)
