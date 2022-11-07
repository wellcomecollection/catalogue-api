def describe_service(sess, *, cluster, service):
    """
    Returns the description of an ECS service.
    """
    ecs = sess.client("ecs")

    resp = ecs.describe_services(cluster=cluster, services=[service])

    if len(resp["services"]) != 1:
        raise ValueError(f"Unexpected response looking up {cluster}/{service}: {resp}")

    return resp["services"][0]


def describe_deployment(sess, *, cluster, service, deployment_id):
    """
    Returns the current state of an ECS deployment.
    """
    service_resp = describe_service(sess, cluster=cluster, service=service)

    try:
        return next(d for d in service_resp["deployments"] if d["id"] == deployment_id)
    except StopIteration:
        raise ValueError(f"No deployment found with id {deployment_id}")


def describe_task_definition(sess, *, task_definition_arn):
    """
    Returns the description of an ECS task definition.
    """
    ecs = sess.client("ecs")

    resp = ecs.describe_task_definition(taskDefinition=task_definition_arn)

    return resp["taskDefinition"]


def redeploy_ecs_service(sess, *, cluster, service):
    """
    Force a new deployment of an ECS service.  Returns the deployment ID.
    """
    ecs = sess.client("ecs")

    resp = ecs.update_service(cluster=cluster, service=service, forceNewDeployment=True)

    return resp["service"]["deployments"][0]["id"]
