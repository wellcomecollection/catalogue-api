# Build this with the repo root as the build context
FROM public.ecr.aws/docker/library/node:16-alpine AS monorepo-builder

WORKDIR /app

COPY ./yarn.lock ./package.json ./
COPY ./concepts ./concepts

WORKDIR concepts
RUN yarn install --frozen-lockfile --production && yarn cache clean

CMD ["yarn", "start"]

