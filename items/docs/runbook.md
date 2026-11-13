# Items and requesting runbook

Shows readers the current availability of library items on wellcomecollection.org and lets signed-in library members request them for viewing.

- **Backplane:** [catalogue-api-items](https://backplane.wellcome.digital/catalog/component/default/catalogue-api-items) and [catalogue-api-requests](https://backplane.wellcome.digital/catalog/component/default/catalogue-api-requests) (internal). The owning team, its contact channel and any suppliers sharing the service are on those pages.
- **Code:** [catalogue-api/items](https://github.com/wellcomecollection/catalogue-api/tree/main/items) and [catalogue-api/requests](https://github.com/wellcomecollection/catalogue-api/tree/main/requests)
- **Last reviewed:** 2026-11-13, first draft

## What it is

Two services, both built and deployed from this repository and both backed by Sierra, the library management system hosted by Innovative. The items API refreshes the availability of items that may have changed since the catalogue pipeline last indexed them. The requests API places holds in Sierra and lists a reader's current requests. Sign-in on the website also depends on Sierra, through the identity service, so a Sierra problem usually breaks sign-in and requesting together.

|                        | Items API                                                                                                                                                                         | Requests API                                                                               |
| ---------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| Runs in                | catalogue account, ECS service `prod-items-api` in cluster `catalogue-api-2021-04-26`, eu-west-1                                                                                  | identity account, ECS service `requests-prod` in cluster `identity-prod`, eu-west-1        |
| Entry point            | `https://api.wellcomecollection.org/catalogue/v2/works/{workId}/items` (needs an API key); the website calls it through `https://wellcomecollection.org/api/works/items/{workId}` | `/users/{userId}/item-requests` on the identity API, which checks the reader's token first |
| Users affected if down | Items show the availability from the last indexing run, so something just requested by another reader can still look requestable                                                  | Readers cannot request items or see their requests on the account page                     |
| Statuspage component   | Requesting & Sign-in                                                                                                                                                              | Requesting & Sign-in                                                                       |

[How users request items](../../docs/requesting_flow.md) has the sequence diagram, and [What happens when you request an item?](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/docs/item-requesting.md) explains the access rules and what library staff do with a request.

The move from Sierra to FOLIO has not reached production. The FOLIO items service exists only as a contract in [items_v3](../../items_v3/README.md), and the FOLIO identity API runs in dev and stage only. Rewrite this runbook when the website switches over ([platform#6637](https://github.com/wellcomecollection/platform/issues/6637), [platform#6638](https://github.com/wellcomecollection/platform/issues/6638)).

## How to tell it is broken

The alarm definitions live in [terraform/modules/stack/alarms.tf](../../terraform/modules/stack/alarms.tf) for the catalogue API and in [infra/scoped/cloudwatch-alarms.tf](https://github.com/wellcomecollection/identity/blob/main/infra/scoped/cloudwatch-alarms.tf) (internal) for the identity API. Neither service has an alarm of its own, and nothing alarms when a Sierra lookup fails, so a problem here often shows up first as a Sierra adapter alert or a user report. Alerts that post to #wc-platform-alerts are triaged by the [bug nurse](https://github.com/wellcomecollection/developer-handbook/blob/main/incident-management/bug-nurse.md).

| Signal                                                                                                                                                                  | Posts to              | What it means                                                                                                                                                                                                                                | Go to                                                                                      |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| `identity-api-prod-5xx-alarm`                                                                                                                                           | #wc-platform-alerts   | More than two 5xx responses in 15 minutes from the identity API, which fronts requesting and sign-in. Usually Sierra, though in October 2026 one reader's hold on a withdrawn item tripped it four times, so check the requests API logs too | [Sierra is slow or down](#sierra-is-slow-or-down)                                          |
| Auth0 "Failed Login" bursts                                                                                                                                             | #wc-platform-alerts   | Sign-in lookups against Sierra are timing out                                                                                                                                                                                                | [Sierra is slow or down](#sierra-is-slow-or-down)                                          |
| `lambda-sierra-adapter-20200604-sierra-reader-*-errors` (the Slack card reads "Lambda errors: sierra-adapter-20200604-sierra-reader-..."), or sierra-reader gap reports | #wc-platform-alerts   | The catalogue pipeline cannot read from Sierra. These Lambdas also run out of memory several times a day without Sierra being at fault, so look for timeouts before treating one firing as an outage                                         | [Sierra is slow or down](#sierra-is-slow-or-down)                                          |
| `catalogue-api-prod-5xx-alarm`, `catalogue-api-prod-latency-alarm`                                                                                                      | #wc-platform-alerts   | More than 100 5xx responses in a minute, or p95 latency over 10 seconds for 3 minutes, across the whole catalogue API gateway. Most of its traffic is search, so an items-only failure rarely trips them                                     | The [search runbook](../../search/docs/runbook.md) first, then [First steps](#first-steps) |
| Reports from colleagues or the public                                                                                                                                   | #wc-platform-feedback | "Can't request", "can't log in", "my requests have disappeared"                                                                                                                                                                              | [First steps](#first-steps)                                                                |

Quick checks anyone can run:

- Items through the website, which should return 200 within a couple of seconds:
  `curl -s -o /dev/null -w "%{http_code} %{time_total}s\n" https://wellcomecollection.org/api/works/items/rp9jnamu`
- Items version: `curl -s https://api.wellcomecollection.org/catalogue/management/items/manifest`. Calling the items endpoint on `api.wellcomecollection.org` directly returns 403 "Invalid API key", which is expected.
- Requesting: sign in on wellcomecollection.org and open the account page. Your current requests should list.
- Logs: Kibana on the logging cluster, and Elastic APM under the service names `items-api` and `requests-api` (internal). The items API logs `Item lookup failed` when Sierra returns an error.

## First steps

1. **Is it real?** Run the quick checks above. One alarm with healthy checks may be noise; say so in the alert thread.
2. **Say you are looking.** Reply in the alert thread, or in #wc-incident-response if users are affected.
3. **Is Sierra the cause?** It is the most common one. Run the check in [Sierra is slow or down](#sierra-is-slow-or-down) before changing anything of ours. In July 2024 a front-end change was reverted during a Sierra outage and turned out to be unrelated.
4. **Did something change?** Look at #wc-platform-deploys and the [catalogue-api prod deploys](https://buildkite.com/wellcomecollection/catalogue-api-deploy-prod) (internal). If a deploy lines up with the start of the problem and Sierra looks healthy, roll back first and investigate after (see [Rollback](#rollback)).
5. **Get access.** Use `catalogue-read_only` for the items API, `identity-read_only` for the requests API and `platform-read_only` for the Sierra adapter logs. [Getting credentials for working locally](https://github.com/wellcomecollection/aws-account-infrastructure/blob/main/docs/how-to/how-to-get-credentials-for-working-locally.md) (internal) explains the set-up. Use a write role only for the fix itself.
6. **Is it major?** If readers cannot sign in or request items and a fix is not in sight, follow the [major incident playbook](https://github.com/wellcomecollection/developer-handbook/blob/main/incident-management/incident-comms-and-escalation.md).

## Common failures and fixes

### Sierra is slow or down

- **Symptom:** Auth0 failed-login bursts, `identity-api-prod-5xx-alarm`, Sierra reader Lambda timeouts, readers reporting they cannot sign in or request. Items fall back to their indexed availability. In September 2026 a single Sierra endpoint (the patron email lookup) stopped answering while the rest stayed fast, so sign-in failed but other calls worked.
- **Confirm:** look for connection or read timeouts from the Sierra reader, which calls Sierra every few minutes:

  ```console
  $ AWS_PROFILE=platform-read_only aws logs tail /aws/lambda/sierra-adapter-20200604-sierra-reader-items \
      --region eu-west-1 --since 1h --filter-pattern '?ConnectTimeout ?ReadTimeout' --format short
  ```

  Timeouts there, with our services' task counts healthy, point at Sierra. Sierra has no public status page.

  ```console
  $ AWS_PROFILE=identity-read_only aws ecs describe-services --region eu-west-1 \
      --cluster identity-prod --services requests-prod --query 'services[].[runningCount,desiredCount]'
  $ AWS_PROFILE=catalogue-read_only aws ecs describe-services --region eu-west-1 \
      --cluster catalogue-api-2021-04-26 --services prod-items-api --query 'services[].[runningCount,desiredCount]'
  ```

- **Cause:** on the Innovative side, or on the network between Wellcome and Innovative. The July 2024 outage followed a Wellcome firewall change that needed Innovative to restart their API.
- **Fix:** nothing in our services fixes it. Tell Collections Product, who raise it with Innovative, and post updates in the incident thread. Open a Statuspage incident on Requesting & Sign-in following the [major incident playbook](https://github.com/wellcomecollection/developer-handbook/blob/main/incident-management/incident-comms-and-escalation.md). If the outage will run for a long time, consider [turning requesting off](#turning-requesting-off).
- **Watch out:** once Sierra recovers, the Sierra adapter may have missed update windows that need re-running so the catalogue catches up; that is in the catalogue pipeline adapter runbook. Check sign-in and requesting yourself before closing the Statuspage incident.
- **Seen in:** [2024-07-25 users cannot log in](https://github.com/wellcomecollection/docs/blob/main/incident_retros/2024-07-25_users_cannot_login_to_wellcomcollection_org.md); the Sierra patron search outage on 2026-09-28 (write-up not yet published); short slowdowns on 2026-10-09, 2026-10-14 and 2026-10-15.

### Turning requesting off

Use this for planned Sierra maintenance or a long outage. The full procedure is [How to disable requesting](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/docs/turn-off-requesting.md) in the website repository, and its message was last edited for the closure that ended on 6 October 2026. Before you start:

- The `disableRequesting` toggle replaces the sign-in prompt and Request button on work pages with the `requestingDisabled` message from [common/data/microcopy.tsx](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/common/data/microcopy.tsx). That message names a reopening date, so it needs editing and a website deploy before you switch the toggle on, which takes time in an unplanned outage.
- Clearing cached work pages needs `experience-admin`, because the CloudFront invalidation runs in the experience account.
- The toggle is shared by prod and stage, and it changes only the website. The items and requests APIs keep running.
- To turn requesting back on, run `yarn setDefaultValueFor --disableRequesting=false` in `toggles/webapp` and repeat steps 4 to 6 of that page.

## Rollback

Follow the general [rollback guide](https://github.com/wellcomecollection/developer-handbook/blob/main/developer-handbook/rollback.md) first. The [change model](https://github.com/wellcomecollection/developer-handbook/blob/main/developer-handbook/change-model.md) says which kinds of change need which route. For these services:

- **How it deploys:** a merge to main builds images tagged `ref.<commit>`. The [stage pipeline](https://buildkite.com/wellcomecollection/catalogue-api-deploy-stage) (internal) moves `env.stage` and redeploys stage. The [prod pipeline](https://buildkite.com/wellcomecollection/catalogue-api-deploy-prod) (internal) waits for someone to unblock "Deploy to prod?", then moves `env.prod` for search, items and concepts together and for requests separately, and redeploys them. Deploys are reported in #wc-platform-deploys.
- **To roll back one service:** point `env.prod` at the previous image and force a redeploy, from a checkout of this repository. Find the previous `ref.` tag first:

  ```console
  $ AWS_PROFILE=catalogue-read_only aws ecr describe-images --region eu-west-1 \
      --repository-name uk.ac.wellcome/items \
      --query 'sort_by(imageDetails,&imagePushedAt)[-5:].[join(`,`,imageTags),imagePushedAt]' --output text
  $ export AWS_PROFILE=catalogue-developer AWS_REGION=eu-west-1
  $ ENV_TAG=env.prod LATEST_TAG=ref.PREVIOUS_COMMIT ./builds/update_ecr_image_tag.sh uk.ac.wellcome/items
  $ CLUSTER=catalogue-api-2021-04-26 ./builds/deploy_ecs_services.sh prod-items-api
  ```

  For the requests API use `identity-read_only` and `identity-developer`, the repository `uk.ac.wellcome/requests`, the cluster `identity-prod` and the service `requests-prod`. ECR keeps only the ten newest builds that no environment is using, so an older image may be gone; revert the pull request instead.

- **Then revert on main:** the next prod deploy moves `env.prod` forward again, so revert the pull request as well.
- **Cannot be rolled back this way:** Terraform changes in [terraform/](../../terraform) (gateway routes, alarms, task sizes) and the identity API's routes in the identity repository, which need a revert and a fresh plan and apply. Website changes follow the [website runbook](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/content/docs/runbook.md). Sierra configuration is Innovative's.
- **Check it worked:** the items manifest shows the old commit (`curl -s https://api.wellcomecollection.org/catalogue/management/items/manifest`). The requests API's manifest is not routed, so check that the newest deployment of `requests-prod` shows `rolloutState` `COMPLETED` in `aws ecs describe-services`, then run the quick checks.

## Dependencies and supplier escalation

Suppliers who build or run part of this service are listed as vendors on the [Backplane entity](https://backplane.wellcome.digital/catalog/component/default/catalogue-api-items) (internal). Their support contacts, contract terms and severity definitions are in [suppliers.md](https://github.com/wellcomecollection/private/blob/main/suppliers.md) (private).

| Dependency             | What breaks if it is down                                                    | How to check it                                                                                                       | Who escalates, and how                                           |
| ---------------------- | ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Sierra (Innovative)    | Sign-in, placing and listing requests, and fresh availability                | Sierra reader logs, as in [Sierra is slow or down](#sierra-is-slow-or-down); no public status page                    | The team tells Collections Product, who raise it with Innovative |
| Catalogue search API   | Both services look up works to map catalogue ids to Sierra ids, so both fail | The [search runbook](../../search/docs/runbook.md)                                                                    | Owning team, via its Backplane team page                         |
| Identity API and Auth0 | Readers cannot sign in, and the requests API cannot be reached               | The [identity runbook](https://github.com/wellcomecollection/identity/blob/main/docs/runbook.md) (internal)           | Owning team, via its Backplane team page                         |
| Content API            | Items lose their viewing dates, so the website cannot confirm a request      | `curl -s -o /dev/null -w "%{http_code}\n" https://api.wellcomecollection.org/content/v0/venues`                       | Owning team, via its Backplane team page                         |
| Website                | Readers see nothing of this                                                  | The [website runbook](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/content/docs/runbook.md) | Owning team, via its Backplane team page                         |

## Related links

- Architecture pages: [items_api](https://github.com/wellcomecollection/docs/blob/main/architecture/services/apis/items_api.md) and [requesting_api](https://github.com/wellcomecollection/docs/blob/main/architecture/services/apis/requesting_api.md)
- [How to disable requesting](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/docs/turn-off-requesting.md) and [What happens when you request an item?](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/docs/item-requesting.md)
- [End-of-year closure](https://github.com/wellcomecollection/developer-handbook/blob/main/end-of-year-closure.md), which covers the requesting date picker over the holidays
- [Items API v3](../../items_v3/README.md), the FOLIO replacement for the items API
- Retros: [2024-07-25 users cannot log in](https://github.com/wellcomecollection/docs/blob/main/incident_retros/2024-07-25_users_cannot_login_to_wellcomcollection_org.md), [2022-05-13 requests not showing in account](https://github.com/wellcomecollection/docs/blob/main/incident_retros/2022-05-13_requests-not-showing-in-account.md)
