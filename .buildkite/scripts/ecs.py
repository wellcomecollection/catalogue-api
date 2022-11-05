def describe_service(sess, *, cluster, service):
    """
    Returns the description of an ECS service.
    """
    ecs = sess.client("ecs")

    resp = ecs.describe_services(cluster=cluster, services=[service])

    if len(resp["services"]) != 1:
        raise ValueError(f"Unexpected response looking up {cluster}/{service}: {resp}")

    return resp["services"][0]


def describe_task_definition(sess, *, task_definition_arn):
    """
    Returns the description of an ECS task definition.
    """
    ecs = sess.client("ecs")

    resp = ecs.describe_task_definition(taskDefinition=task_definition_arn)

    return resp["taskDefinition"]


def describe_running_tasks_in_service(sess, *, cluster, service):
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
            cluster=cluster, tasks=task_arns, include=["TAGS"]
        )

        return resp["tasks"]
    else:
        return []


def get_desired_task_count(sess, *, cluster, service):
    """
    Returns the desired task count of a service.
    """
    service = describe_service(sess, cluster=cluster, service=service)
    return service["desiredCount"]


def redeploy_ecs_service(sess, *, cluster, service):
    """
    Force a new deployment of an ECS service.  Returns the deployment ID.
    """
    ecs = sess.client("ecs")

    resp = ecs.update_service(cluster=cluster, service=service, forceNewDeployment=True)

    return resp["service"]["deployments"][0]["id"]
