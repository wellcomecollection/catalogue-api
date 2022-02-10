# This Dockerfile is run with the ./ (root of repo) as context
# We do this as we need to copy in files from around the repo
FROM 760097843905.dkr.ecr.eu-west-1.amazonaws.com/python:3.7-alpine

LABEL maintainer = "Wellcome Collection <dev@wellcomecollection.org>"
LABEL description = "A Docker image for deploying our Docker images to AWS"

WORKDIR /usr/src/app

ADD ./internal_model_tool/requirements.txt requirements.txt
RUN pip install -r requirements.txt

ADD ./internal_model_tool/common.py common.py
ADD ./internal_model_tool/check_compatibility.py check_compatibility.py
ADD ./common/config/src/main/scala/weco/catalogue/config/ElasticConfig.scala ../common/config/src/main/scala/weco/catalogue/config/ElasticConfig.scala
ADD ./project/Dependencies.scala ../project/Dependencies.scala

CMD ["/usr/local/bin/python3", "/usr/src/app/check_compatibility.py"]
