# Search API runbook

The search API serves works and images from the collections catalogue, to collections search on wellcomecollection.org and to anyone using the public catalogue API.

- **Backplane:** [catalogue-api-search](https://backplane.wellcome.digital/catalog/component/default/catalogue-api-search) (internal). The owning team, its contact channel and any suppliers sharing the service are on that page.
- **Code:** [catalogue-api/search](https://github.com/wellcomecollection/catalogue-api/tree/main/search)
- **Last reviewed:** 2026-11-13, first draft

## What it is

An ECS Fargate service in the catalogue AWS account. Requests reach it through CloudFront on `api.wellcomecollection.org` (platform account), then the "Catalogue API (prod)" API Gateway and a network load balancer, which it shares with the items and concepts APIs. It reads the works and images indexes of the live catalogue pipeline from Elasticsearch in Elastic Cloud, over PrivateLink. When it is down, collections search and work pages on wellcomecollection.org fail along with it, because the site builds those pages from this API. The [architecture page](https://github.com/wellcomecollection/docs/blob/main/architecture/services/apis/search_api.md) has a diagram.

|                           |                                                                                                                      |
| ------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Runs in                   | catalogue account, ECS service `prod-search-api` in cluster `catalogue-api-2021-04-26`, eu-west-1                    |
| Public URL or entry point | https://api.wellcomecollection.org/catalogue/v2/works and `/images`                                                  |
| Users affected if down    | search and work pages on wellcomecollection.org return errors; external users of the catalogue API get 5xx responses |
| Statuspage component      | Collections Search                                                                                                   |

### Capacity

Prod runs a fixed three tasks of 1 vCPU and 2 GB each, with no autoscaling. Autoscaling was considered after the September 2026 outage and declined as too complex for what it would buy ([platform-infrastructure#547](https://github.com/wellcomecollection/platform-infrastructure/issues/547), internal). The JVM runs with its default heap, a quarter of the task memory, and exits on `OutOfMemoryError` so that ECS replaces the task ([#982](https://github.com/wellcomecollection/catalogue-api/pull/982)).

The count of three is set in [`terraform/catalogue_api/main.tf`](https://github.com/wellcomecollection/catalogue-api/blob/main/terraform/catalogue_api/main.tf), but the shared ECS service module ignores `desired_count` once a service exists, so editing Terraform does not change a running service. To change it, use `aws ecs update-service --desired-count` (see [Traffic floods](#traffic-floods-saturate-the-three-tasks)) and record the change in the incident issue, because nothing will set it back for you.

Stage also runs three tasks, on Fargate Spot, and is stopped outside office hours. Stage 5xx errors overnight are expected, which is why there are no stage alarms.

The Elasticsearch cluster is the live pipeline's cluster, which the catalogue pipeline also writes to. Its size per zone and number of zones are set in [`infrastructure/critical/es_cluster.tf`](https://github.com/wellcomecollection/catalogue-pipeline/blob/main/infrastructure/critical/es_cluster.tf) in catalogue-pipeline. Sizing is the owning team's decision, made through a pull request to that file.

## How to tell it is broken

The alarm definitions (thresholds and routing) live in [`terraform/modules/stack/alarms.tf`](https://github.com/wellcomecollection/catalogue-api/blob/main/terraform/modules/stack/alarms.tf). Both alarms measure the whole catalogue API gateway, so they also fire for items and concepts errors. They notify once, when they go into ALARM, and send nothing when they recover. Alerts that post to #wc-platform-alerts are triaged by the [bug nurse](https://github.com/wellcomecollection/developer-handbook/blob/main/incident-management/bug-nurse.md).

| Signal                                        | Posts to              | What it means                                                                                                                        | Go to                                                                                                             |
| --------------------------------------------- | --------------------- | ------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------- |
| `catalogue-api-prod-5xx-alarm`                | #wc-platform-alerts   | More than 100 5xx responses in a minute. Normal background bursts reach about 50.                                                    | [Stuck tasks](#tasks-stay-up-but-their-searches-fail), [Traffic floods](#traffic-floods-saturate-the-three-tasks) |
| `catalogue-api-prod-latency-alarm`            | #wc-platform-alerts   | p95 latency over 10 seconds for 3 minutes. Searches are queueing in the tasks or in Elasticsearch.                                   | [Elasticsearch under pressure](#elasticsearch-under-pressure)                                                     |
| LogicMonitor web checks on collections search | #wc-platform-alerts   | Website search pages failing from outside. The `query=botany` and `query=skeletons` requests in logs are these checks.               | [First steps](#first-steps)                                                                                       |
| Snapshot report                               | #wc-search-alerts     | Weekday morning comparison of index counts with the API. A warning means the pipeline has stopped updating, not that search is down. | [Dependencies](#dependencies-and-supplier-escalation)                                                             |
| Reports from colleagues or the public         | #wc-platform-feedback | "Search is not working" or "no results", often with a screenshot of the website error page                                           | [First steps](#first-steps)                                                                                       |

Quick checks anyone can run:

- Version and start time: `curl -s https://api.wellcomecollection.org/catalogue/management/search/manifest`
- A real search: `curl -s -o /dev/null -w '%{http_code} %{time_total}s\n' 'https://api.wellcomecollection.org/catalogue/v2/works?query=cat'`, and the same for `/images?query=cat`. Expect 200 in about a second or less.
- Which pipeline and indexes it is reading: `curl -s https://api.wellcomecollection.org/catalogue/v2/_elasticConfig`
- `/management/healthcheck` returns `["message","ok"]` without touching Elasticsearch, so a passing health check does not mean search works.
- Dashboard: the API Gateway metrics for "Catalogue API (prod)" and the ECS service metrics for `prod-search-api` in the CloudWatch console (internal).
- Logs and traces: Kibana in the logging deployment, searching for `prod-search-api`, and APM service `search-api` in environment `prod` (internal).

## First steps

1. **Is it real?** Run the quick checks above. One alarm with healthy checks may be noise; say so in the alert thread.
2. **Say you are looking.** Reply in the alert thread, or in #wc-incident-response if users are affected.
3. **Did something change?** Look at recent prod deploys in #wc-platform-deploys or the [prod deploy pipeline](https://buildkite.com/wellcomecollection/catalogue-api-deploy-prod) (internal), and at merged PRs here and in catalogue-pipeline (a pipeline Terraform apply can change the Elasticsearch API key or cluster). If a deploy lines up with the start of the problem, roll back first and investigate after (see [Rollback](#rollback)).
4. **Is a dependency down?** Check the status pages in [Dependencies](#dependencies-and-supplier-escalation).
5. **Get access.** Start with the `catalogue-read_only` role. The [guide to selecting a role in the CLI](https://github.com/wellcomecollection/aws-account-infrastructure/blob/main/docs/how-to/how-to-select-a-role-in-the-cli.md) (internal) explains how. Use `catalogue-developer` only for the fix itself.
6. **Is it major?** If people cannot search the collections and a fix is not in sight, follow the [major incident playbook](https://github.com/wellcomecollection/developer-handbook/blob/main/incident-management/incident-comms-and-escalation.md).

To see the tasks and how long each has been running:

```bash
export AWS_PROFILE=catalogue-read_only AWS_REGION=eu-west-1
aws ecs describe-services --cluster catalogue-api-2021-04-26 --services prod-search-api \
  --query 'services[0].{desired:desiredCount,running:runningCount,events:events[:5].message}'
aws ecs list-tasks --cluster catalogue-api-2021-04-26 --service-name prod-search-api
```

## Common failures and fixes

### Tasks stay up but their searches fail

- **Symptom:** the 5xx alarm, and searches failing for some or most users. A share of requests still work if only some tasks are affected.
- **Confirm:** in the logs, errors come from one or two task ids rather than all three, with `CancellationException: Request execution cancelled` or `OutOfMemoryError`. The task is still listed as running and healthy, because the health check does not touch Elasticsearch.
- **Cause:** a burst of heavy requests exhausts the JVM heap. An out-of-memory error on the Elasticsearch client's I/O thread leaves the task alive but unable to reach Elasticsearch. Since [#982](https://github.com/wellcomecollection/catalogue-api/pull/982) the JVM exits on out-of-memory and ECS replaces it, so if you see this again the cause is probably different; write down what you find.
- **Fix:** replace the bad tasks. A rolling replacement of all three, which starts new tasks before stopping old ones:

  ```bash
  AWS_PROFILE=catalogue-developer aws ecs update-service --region eu-west-1 \
    --cluster catalogue-api-2021-04-26 --service prod-search-api --force-new-deployment
  ```

  Or stop just the bad one and let ECS start a replacement: `AWS_PROFILE=catalogue-developer aws ecs stop-task --region eu-west-1 --cluster catalogue-api-2021-04-26 --task "TASK_ID"`.

- **Watch out:** stopping all three tasks at once leaves nothing serving. If whatever caused the burst is still running, the new tasks can fail the same way; see [Traffic floods](#traffic-floods-saturate-the-three-tasks).
- **Seen in:** [2024-02-26 retro](https://github.com/wellcomecollection/docs/blob/main/incident_retros/2024-02-26_search_not_available.md), [2026-09-13 retro](https://github.com/wellcomecollection/docs/blob/main/incident_retros/2026-09-13_collections_search_not_available.md) and [platform-infrastructure#547](https://github.com/wellcomecollection/platform-infrastructure/issues/547) (internal)

### Traffic floods saturate the three tasks

- **Symptom:** the 5xx alarm, the latency alarm, or both, with CPU on all three tasks near 100%.
- **Confirm:** CPU utilisation for `prod-search-api` in CloudWatch, and the request count on the API Gateway. In the logs, look for one user agent or a small set of IPs making a large share of the requests.
- **Cause:** bots and scrapers, usually arriving through wellcomecollection.org search pages. The website's WAF is the main defence.
- **Fix:** block or challenge the traffic at the website WAF, which belongs to wellcomecollection.org (see its [runbook](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/content/docs/runbook.md#bot-or-scraper-traffic-slows-or-breaks-the-site) and [platform#6428](https://github.com/wellcomecollection/platform/issues/6428) for the emergency blocklist). To add capacity while that takes effect, scale the service out:

  ```bash
  AWS_PROFILE=catalogue-developer aws ecs update-service --region eu-west-1 \
    --cluster catalogue-api-2021-04-26 --service prod-search-api --desired-count 6
  ```

- **Watch out:** more tasks send more queries to Elasticsearch, so if the cluster is the bottleneck this makes it worse (check [Elasticsearch under pressure](#elasticsearch-under-pressure) first). Set the count back to 3 once traffic is normal, and note both changes in the incident issue.
- **Seen in:** [platform#6428](https://github.com/wellcomecollection/platform/issues/6428) (6 July 2026, no alert fired, which led to both alarms above)

### Elasticsearch under pressure

- **Symptom:** the latency alarm, then 5xx errors after about a 20 second wait. All three tasks are affected equally and their CPU is not pegged.
- **Confirm:** the deployment's health and its node memory and CPU in the Elastic Cloud console (internal), and garbage collection lines in its logs. Elastic also emails "node restarted after running out of memory" notices to the deployment's contacts.
- **Cause:** usually a burst of aggregation-heavy searches from a bot, against a cluster with little headroom. A full reindex in the pipeline also loads the same cluster.
- **Fix:** deal with the traffic first (see [Traffic floods](#traffic-floods-saturate-the-three-tasks)). If the cluster needs more memory, change `memory` or `node_count` in catalogue-pipeline's [`es_cluster.tf`](https://github.com/wellcomecollection/catalogue-pipeline/blob/main/infrastructure/critical/es_cluster.tf) and apply it. Resizing in the Elastic Cloud console is quicker, but make Terraform match straight after or the next apply will undo it.
- **Watch out:** resizing an Elastic Cloud deployment moves shards and adds load while it runs. The cluster is shared with the pipeline, so tell the pipeline owners before resizing during a reindex.
- **Seen in:** 27 September 2026, a four-minute outage on a cluster of the same size that recovered on its own (no retro yet)

### Elasticsearch rejects the API's credentials

- **Symptom:** every works or images request fails at once, stage as well as prod, often just after a catalogue-pipeline Terraform apply.
- **Confirm:** `security_exception` and "unable to authenticate with provided credentials" in the logs. Check when the key last changed:

  ```bash
  AWS_PROFILE=catalogue-read_only aws secretsmanager list-secret-version-ids --region eu-west-1 \
    --secret-id "elasticsearch/pipeline_storage_PIPELINE_DATE/catalogue_api/api_key"
  ```

  where `PIPELINE_DATE` is the `pipelineDate` from `_elasticConfig`.

- **Cause:** a pipeline Terraform change recreated the API key. Since [#871](https://github.com/wellcomecollection/catalogue-api/pull/871) the client rebuilds itself after a 401 or 403, so this should now recover without help.
- **Fix:** if it does not recover within a few minutes, force a new deployment (as in [Tasks stay up](#tasks-stay-up-but-their-searches-fail)) so the tasks read the new secret.
- **Watch out:** the snapshot generator and the concepts API have their own keys, made by the same Terraform, so check them too.
- **Seen in:** [2025-11-20 retro](https://github.com/wellcomecollection/docs/blob/main/incident_retros/2025-11-20_cannot_search_works_and_images.md)

## Rollback

Follow the general [rollback guide](https://github.com/wellcomecollection/developer-handbook/blob/main/developer-handbook/rollback.md) first. The [change model](https://github.com/wellcomecollection/developer-handbook/blob/main/developer-handbook/change-model.md) says which kinds of change need which route. For this service:

- **How it deploys:** merging to `main` builds and pushes an image tagged `ref.COMMIT`. The [stage pipeline](https://buildkite.com/wellcomecollection/catalogue-api-deploy-stage) (internal) points `env.stage` at it, redeploys the stage search, items, concepts and requests services, and runs smoke, end-to-end and rank tests. The [prod pipeline](https://buildkite.com/wellcomecollection/catalogue-api-deploy-prod) (internal) runs the diff tool and waits at "Deploy to prod?". Unblocking it points `env.prod` at the same commit and redeploys the prod search, items and concepts services, plus requests and the snapshot generator. The deploy tracker posts each deploy to #wc-platform-deploys. [developers.md](../../docs/developers.md) describes the flow.
- **To roll back a code change:** revert the PR, merge it, and unblock the prod deploy once stage is green. If that is too slow, point `env.prod` for search alone back at the last good commit (from #wc-platform-deploys or the repository's prod Deployments) and redeploy it, from the repository root:

  ```bash
  export AWS_PROFILE=catalogue-developer AWS_REGION=eu-west-1
  ENV_TAG="env.prod" LATEST_TAG="ref.GOOD_COMMIT" ./builds/update_ecr_image_tag.sh uk.ac.wellcome/search
  CLUSTER="catalogue-api-2021-04-26" ./builds/deploy_ecs_services.sh prod-search-api
  ```

  Revert the PR as well, or the next merge to `main` redeploys the bad change. A manual retag creates no GitHub Deployment, so the deploy tracker and Backplane do not see it; note it in the incident issue.

- **Cannot be rolled back this way:**
  - A pipeline flip, which changes `ElasticConfig.scala` and `concepts/config.ts`. Revert both and redeploy, as [multi-cluster-setup.md](../../docs/multi-cluster-setup.md#flipping-the-default) describes. This only works while the previous pipeline's indexes and API key still exist, so check them in Elastic Cloud before relying on it.
  - Terraform changes to the gateway, load balancer, task size or alarms. Revert the PR, merge it, then plan and apply `terraform/catalogue_api` from `main`, as the [rollback guide](https://github.com/wellcomecollection/developer-handbook/blob/main/developer-handbook/rollback.md#terraform) describes. Applying the pre-change commit because it cannot wait is an emergency change.
  - Elasticsearch cluster size, indexes and API keys, which live in catalogue-pipeline.
- **Check it worked:** the `manifest` quick check shows the commit you deployed, and `_elasticConfig` shows the pipeline you expect.

### Previewing another cluster

The API can route single requests to a second Elasticsearch cluster with `?elasticCluster=NAME`, configured in [`application.conf`](https://github.com/wellcomecollection/catalogue-api/blob/main/search/src/main/resources/application.conf). It is for previewing a new pipeline before a flip, and does not change what website users see. The current preview is `folio-testing`. [multi-cluster-setup.md](../../docs/multi-cluster-setup.md) explains how to add, flip and remove one.

## Dependencies and supplier escalation

Suppliers who build or run part of this service are listed as vendors on the [Backplane entity](https://backplane.wellcome.digital/catalog/component/default/catalogue-api-search) (internal). Their support contacts, contract terms and severity definitions are in [suppliers.md](https://github.com/wellcomecollection/private/blob/main/suppliers.md) (private).

| Dependency                                          | What breaks if it is down                                                                                                                           | How to check it                                                                                                                                      | Who escalates, and how                                                                                                                                                                                                                                                     |
| --------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Elastic Cloud                                       | every search fails or is slow ([2021-02-24 retro](https://github.com/wellcomecollection/docs/blob/main/incident_retros/2021-02-24_ElasticCloud.md)) | [status.elastic.co](https://status.elastic.co) and the deployment page in the Elastic Cloud console (internal)                                       | Owning team raises a support ticket in the Elastic Cloud console. The incident lead sets its severity, as the [major incident playbook](https://github.com/wellcomecollection/developer-handbook/blob/main/incident-management/incident-comms-and-escalation.md) describes |
| Catalogue pipeline                                  | nothing goes down; new and changed records stop appearing                                                                                           | the weekday snapshot report in #wc-search-alerts; [catalogue-pipeline docs](https://github.com/wellcomecollection/catalogue-pipeline/tree/main/docs) | Owning team, via the [catalogue-pipeline System](https://backplane.wellcome.digital/catalog/system/default/catalogue-pipeline) (internal)                                                                                                                                  |
| wellcomecollection.org WAF                          | bot traffic reaches the API unfiltered                                                                                                              | the [website runbook](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/content/docs/runbook.md)                                | Owning team, via the [wellcomecollection-org System](https://backplane.wellcome.digital/catalog/system/default/wellcomecollection-org) (internal)                                                                                                                          |
| AWS (CloudFront, API Gateway, ECS, Secrets Manager) | requests fail before or after reaching the tasks                                                                                                    | [AWS Health Dashboard](https://health.aws.amazon.com/health/status)                                                                                  | Owning team, through AWS Support in the console                                                                                                                                                                                                                            |

## Related links

- Architecture page: [docs/architecture/services/apis/search_api](https://github.com/wellcomecollection/docs/blob/main/architecture/services/apis/search_api.md)
- [Elasticsearch multi-cluster support](../../docs/multi-cluster-setup.md), for pipeline previews and flips
- [platform-infrastructure#547](https://github.com/wellcomecollection/platform-infrastructure/issues/547) (internal), which records why there is no autoscaling and no explicit heap size
- [platform#6428](https://github.com/wellcomecollection/platform/issues/6428), which records why the alarm thresholds are what they are and why stage has none
- [Out-of-hours support](https://github.com/wellcomecollection/developer-handbook/blob/main/incident-management/out-of-hours-support.md): the alarms post to Slack only, so a weekend outage waits for office hours unless someone happens to see it
- [Items and requesting runbook](../../items/docs/runbook.md): the items API shares this gateway and its alarms. The concepts API shares them too and has no runbook yet.
