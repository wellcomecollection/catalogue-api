# This image should be built with the parent directory as context
FROM 760097843905.dkr.ecr.eu-west-1.amazonaws.com/node:12.18.3

WORKDIR /usr/src/app/webapp

ADD package.json package.json
ADD yarn.lock yarn.lock

RUN yarn install

ADD processorFunctions.js processorFunctions.js
ADD catalogueApi.yaml catalogueApi.yaml

CMD ["true"]
