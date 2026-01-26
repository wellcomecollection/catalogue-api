# Build this with the repo root as the build context (docker build . --file search_v2/Dockerfile)
FROM public.ecr.aws/docker/library/node:22-alpine

WORKDIR /app

COPY ./yarn.lock ./package.json ./
COPY ./search_v2 ./search_v2

WORKDIR search_v2
RUN yarn install --frozen-lockfile && yarn cache clean

CMD ["yarn", "start"]
