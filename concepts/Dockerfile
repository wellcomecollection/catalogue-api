# Build this with the repo root as the build context (docker build . --file concepts/Dockerfile)
FROM public.ecr.aws/docker/library/node:20-alpine

WORKDIR /app

COPY ./yarn.lock ./package.json ./
COPY ./concepts ./concepts

# openapi.test.ts reads this to check the spec still matches the routes we serve.
COPY ./reference ./reference

# openapi.test.ts validates responses against the pipeline-generated concept
# fixtures. Only concepts.* is copied, so the frequently-synced works and
# images fixtures don't invalidate this layer.
COPY ./common/search/src/test/resources/test_documents/concepts.*.json ./common/search/src/test/resources/test_documents/

WORKDIR concepts
RUN yarn install --frozen-lockfile && yarn cache clean

CMD ["yarn", "start"]

