# This image should be built with the parent directory as context
FROM 760097843905.dkr.ecr.eu-west-1.amazonaws.com/node:12.18.3

WORKDIR /usr/src/app/webapp

ADD package.json package.json
ADD yarn.lock yarn.lock

RUN yarn install

ADD processorFunctions.js processorFunctions.js
ADD smokeTests.yml smokeTests.yml
ADD stressTests.yml stressTests.yml
ADD ids.csv ids.csv
ADD queries.csv queries.csv

CMD ["true"]
