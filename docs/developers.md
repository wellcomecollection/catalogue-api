### Continuous integration

The [current latest default branch](https://buildkite.com/wellcomecollection/catalogue-api) build [deploys to staging automatically](https://buildkite.com/wellcomecollection/catalogue-api-deploy-stage).

After a deployment to stage environment, we run [smoke tests](smoke_tests/README.md) against the stage API and then [e2e tests](https://github.com/wellcomecollection/wellcomecollection.org/blob/main/playwright/README.md) on the front-end pointing the production wellcomecollection.org at the stage catalogue API.

After a successful stage deployment we run the [diff_tool](diff_tool/README.md) and wait for a user to review and [deploy to production](https://buildkite.com/wellcomecollection/catalogue-api-deploy-prod).

We then run the same smoke & e2e tests pointed at production to confirm a successful deployment.

The CI flow looks as follows:

![Buildkite pipelines](buildkite_flow.png)

## Dependencies

- Java 1.8
- Scala 2.12
- SBT
- Terraform
- Docker
- Make

### Installing dependencies

We suggest using [SDKMAN](https://sdkman.io/) to manage Java versions. It can be installed with:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Then install Java 11 with:

```bash
sdk install java 11.0.24-amzn
```

At the root of the project you should be able to use `sbt` to run the project as described below.

## Running locally

Currently only the search & items API can be run locally. It will use the configured pipeline index in
[`ElastiConfig.scala`](../common/search/src/main/scala/weco/api/search/models/ElasticConfig.scala).

You will need to have signed in to the AWS on the CLI to allow the application to assume the required role.

To run with reloading of code changes using [`sbt-revolver`](https://github.com/spray/sbt-revolver) from the root of the repository:

```bash
sbt "project search" "~reStart"
AWS_PROFILE=catalogue-developer sbt "project items" "~reStart"
```

You should then be able to access the APIs at:

- `http://localhost:8080/works`: Seach
- `http://localhost:8081/items`: Items

To specify a different pipeline index, you can set the `pipelineDate` environment variable for the search API:

```bash
pipelineDate=2021-01-01 sbt "project search" "~reStart"
```
