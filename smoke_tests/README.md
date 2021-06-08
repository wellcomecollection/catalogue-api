# smoke_tests

This directory contains [artillery.io](https://artillery.io/) configuration to run post service deployment smoke tests.

These tests should run after a deployment to ensure that things are behaving as expected in the deployed environment.

In addition, this directory contains stress testing configuration to enable load testing when required.

## Running smoke tests

```
# Install dependencies
yarn install

# AWS credentials are required to retrieve secrets 
# in the Experience AWS account for load testing 
# authenticated APIs
export AWS_REGION=eu-west-1 
export AWS_PROFILE=experience 

# Run smoke tests against stage
yarn smokeTestCatalogueApiStage

# Run smoke tests against prod
yarn smokeTestCatalogueApiProd
```

## Running stress tests

**Important:** Ensure that you are not going to put undue load on the production services (or dependent Elastic clusters)! Lack of due care could break the catalogue API!

To stop a running test you can Ctrl-C to interrupt execution.

```
# Run stress tests against stage
yarn stressTestCatalogueApiStage

# Run stress tests against prod
yarn stressTestCatalogueApiProd
```
