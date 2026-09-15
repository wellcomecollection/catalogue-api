FROM public.ecr.aws/docker/library/eclipse-temurin:11-jre-alpine

LABEL maintainer = "Wellcome Collection <digital@wellcomecollection.org>"

RUN apk add --no-cache bash

ADD target/universal/stage /opt/docker

# Served at /management/manifest so deploy tooling can see which commit is live.
# After the ADD so a new commit only invalidates this layer.
ARG BUILD_COMMIT
ENV BUILD_COMMIT=$BUILD_COMMIT

ENTRYPOINT ["/opt/docker/bin/requests"]
