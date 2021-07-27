FROM 760097843905.dkr.ecr.eu-west-1.amazonaws.com/python:3.7-alpine

LABEL maintainer = "Wellcome Collection <dev@wellcomecollection.org>"
LABEL description = "A Docker image for deploying our Docker images to AWS"

WORKDIR /usr/src/app

ADD requirements.txt requirements.txt
RUN pip install -r requirements.txt

ADD common.json common.json
ADD check_compatibility.py check_compatibility.py

CMD ["/usr/local/bin/python3", "/usr/src/app/check_compatibility.py"]
