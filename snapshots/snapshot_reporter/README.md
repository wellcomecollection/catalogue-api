To upload a new Lambda deployment package:

```console
pip3 install --target ./src -r requirements.txt
cd src/
zip -r ../snapshot_reporter.zip .
cd ..
AWS_PROFILE=catalogue-dev aws s3 cp snapshot_reporter.zip s3://wellcomecollection-catalogue-infra-delta/lambdas/snapshots/snapshot_reporter.zip
```
