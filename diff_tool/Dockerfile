FROM public.ecr.aws/docker/library/python:3.12-alpine

LABEL maintainer = "Wellcome Collection <dev@wellcomecollection.org>"
LABEL description = "A Docker image for deploying our Docker images to AWS"

WORKDIR /usr/src/app

ADD requirements.txt requirements.txt
RUN pip install -r requirements.txt

ADD routes.json routes.json
ADD template.html template.html
ADD api_stats.py api_stats.py
ADD diff_tool.py diff_tool.py

CMD ["/usr/local/bin/python3", "/usr/src/app/diff_tool.py", "--console", "--outfile", "/usr/src/app/reports/api_diff.txt"]
