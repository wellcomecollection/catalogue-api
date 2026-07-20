# Build this with the repo root as the build context (docker build . --file concepts/Dockerfile)
FROM public.ecr.aws/docker/library/node:20-alpine

WORKDIR /app

COPY ./yarn.lock ./package.json ./
COPY ./concepts ./concepts

# openapi.test.ts reads this to check the spec still matches the routes we serve.
COPY ./reference ./reference

WORKDIR concepts
RUN yarn install --frozen-lockfile && yarn cache clean

CMD ["yarn", "start"]

