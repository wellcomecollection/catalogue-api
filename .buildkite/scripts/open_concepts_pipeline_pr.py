#!/usr/bin/env python3
"""
The concepts pipeline needs to subscribe to the catalogue pipeline;
currently it has the name of the catalogue pipeline in its Terraform.

When we deploy a new catalogue API, this opens a PR on the concepts
pipeline repo to point it at the new pipeline.
"""

import contextlib
import json
import os
import re
import shutil
import tempfile
import urllib.request

import boto3
import httpx

from commands import git


def current_pipeline_date():
    resp = urllib.request.urlopen(
        "https://api.wellcomecollection.org/catalogue/v2/_elasticConfig"
    )

    # this will be a string of the form 'works-indexed-YYYY-MM-DD'
    works_index = json.load(resp)["worksIndex"]

    m = re.search(r"\d{4}-\d{2}-\d{2}", works_index)
    if m is None:
        raise RuntimeError(f"Could not work out what the current pipeline date is!")

    return m.group(0)


@contextlib.contextmanager
def working_directory(path):
    """
    Changes the working directory to the given path, then returns to the
    original directory when done.
    """
    prev_cwd = os.getcwd()
    os.chdir(path)
    try:
        yield
    finally:
        os.chdir(prev_cwd)


@contextlib.contextmanager
def cloned_repo(git_url):
    """
    Clones the repository and changes the working directory to the cloned
    repo.  Cleans up the clone when it's done.
    """
    repo_dir = tempfile.mkdtemp()

    git("clone", git_url, repo_dir)

    try:
        with working_directory(repo_dir):
            yield
    finally:
        shutil.rmtree(repo_dir)


class AlreadyAtLatestVersionException(Exception):
    pass


def update_catalogue_pipeline_version(*, pipeline_date):
    old_lines = list(open("infrastructure/main.tf"))

    with open("infrastructure/main.tf", "w") as out_file:
        for line in old_lines:
            if line.startswith("  catalogue_namespace = "):
                new_line = f'  catalogue_namespace = "{pipeline_date}" // This is automatically bumped by the catalogue-api repo\n'

                if new_line == line:
                    raise AlreadyAtLatestVersionException()

                out_file.write(new_line)
            else:
                out_file.write(line)


def get_github_api_key():
    session = boto3.Session()
    secrets_client = session.client("secretsmanager")

    secret_value = secrets_client.get_secret_value(
        SecretId="builds/github_wecobot/scala_libs_pr_bumps"
    )

    return secret_value["SecretString"]


def create_concepts_pipeline_pull_request(*, pipeline_date):
    with cloned_repo("git@github.com:wellcomecollection/concepts-pipeline.git"):
        try:
            update_catalogue_pipeline_version(pipeline_date=pipeline_date)
        except AlreadyAtLatestVersionException:
            return

        branch_name = f"point-concepts-pipeline-at-{pipeline_date}"

        git("config", "--local", "user.email", "wellcomedigitalplatform@wellcome.ac.uk")
        git(
            "config",
            "--local",
            "user.name",
            "BuildKite on behalf of Wellcome Collection",
        )

        git("checkout", "-b", branch_name)
        git("add", "infrastructure/main.tf")
        git("commit", "-m", f"Point concepts-pipeline at {pipeline_date}")
        git("push", "origin", branch_name)

        api_key = get_github_api_key()

        client = httpx.Client(auth=("weco-bot", api_key))

        r = client.post(
            f"https://api.github.com/repos/wellcomecollection/concepts-pipeline/pulls",
            headers={"Accept": "application/vnd.github.v3+json"},
            json={
                "head": branch_name,
                "base": "main",
                "title": f"Point concepts-pipeline at {pipeline_date}",
                "maintainer_can_modify": True,
                "body": "",
            },
        )

        try:
            r.raise_for_status()
            new_pr_number = r.json()["number"]
        except Exception:
            print(r.json())
            raise

        r = client.post(
            f"https://api.github.com/repos/wellcomecollection/concepts-pipeline/pulls/{new_pr_number}/requested_reviewers",
            headers={"Accept": "application/vnd.github.v3+json"},
            json={"team_reviewers": ["scala-reviewers"]},
        )

        print(r.json())

        try:
            r.raise_for_status()
        except Exception:
            raise


if __name__ == "__main__":
    create_concepts_pipeline_pull_request(pipeline_date=current_pipeline_date())
