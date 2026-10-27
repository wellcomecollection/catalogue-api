# Build this with the repo root as the build context (docker build . --file concepts/Dockerfile)
FROM public.ecr.aws/docker/library/node:24-alpine@sha256:06d7adf3e0ef61a13f1d28682726366f97a15aa1888c934d3ecec86c33918b53 AS base

WORKDIR /app

COPY ./yarn.lock ./package.json ./
COPY ./concepts/package.json ./concepts/

# CI runs `yarn test` in this stage; it is never published.
FROM base AS test

# openapi.test.ts reads this to check the spec still matches the routes we serve.
COPY ./reference ./reference

# openapi.test.ts validates responses against the pipeline-generated concept
# fixtures. Only concepts.* is copied, so the frequently-synced works and
# images fixtures don't invalidate this layer.
COPY ./common/search/src/test/resources/test_documents/concepts.*.json ./common/search/src/test/resources/test_documents/

RUN yarn install --frozen-lockfile && yarn cache clean

# Paths are listed so a local concepts/node_modules never lands in the image.
COPY ./concepts/config.ts ./concepts/server.ts ./concepts/tsconfig.json ./concepts/jest.config.js ./concepts/
COPY ./concepts/src ./concepts/src
COPY ./concepts/test ./concepts/test

WORKDIR /app/concepts
CMD ["yarn", "test"]

# Published image: production dependencies only, so test and tooling packages don't ship.
FROM base AS runtime

RUN yarn install --frozen-lockfile --production && yarn cache clean

# The app runs under yarn; the base image's bundled npm only adds scanner findings.
RUN rm -rf /usr/local/lib/node_modules/npm /usr/local/bin/npm /usr/local/bin/npx

COPY ./concepts/config.ts ./concepts/server.ts ./concepts/tsconfig.json ./concepts/
COPY ./concepts/src ./concepts/src

WORKDIR /app/concepts

# Served at /management/manifest so deploy tooling can see which commit is live.
# Last so a new commit only invalidates this layer, not the install.
ARG BUILD_COMMIT
ENV BUILD_COMMIT=$BUILD_COMMIT

CMD ["yarn", "start"]
