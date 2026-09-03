# The shared docker-build-and-push action passes --build-arg pythonversion,
# read from .python-version. The default keeps a plain `docker build` working.
ARG pythonversion=3.12
FROM public.ecr.aws/lambda/python:${pythonversion} AS identifiers

LABEL maintainer="Wellcome Collection <digital@wellcomecollection.org>"

# Set working directory
WORKDIR /app

# Copy dependency files
COPY pyproject.toml uv.lock ./

# Install uv
RUN pip install uv

# Install dependencies before the source, so the layer survives source changes.
RUN uv export --frozen --no-default-groups --no-emit-project --no-hashes -o requirements.txt \
    && uv pip install --system -r requirements.txt

# Copy application source code. `core` and `adapters` are imported as top-level
# modules, so the contents of src/ go directly into the task root.
COPY src/ ${LAMBDA_TASK_ROOT}

ENV IDENTIFIERS_BACKEND=rds

CMD [ "adapters.handler.handler" ]
