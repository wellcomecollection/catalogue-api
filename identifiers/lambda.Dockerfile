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

# Install boto3 from the `rds` dependency group, which is where it lives because
# only the RDS Data API backend needs it. --no-emit-project keeps the export to
# dependencies, so this runs before the source is copied in.
RUN uv export --frozen --only-group rds --no-emit-project --no-hashes -o requirements.txt \
    && uv pip install --system -r requirements.txt

# Copy application source code. `core` and `adapters` are imported as top-level
# modules, so the contents of src/ go directly into the task root.
COPY src/ ${LAMBDA_TASK_ROOT}

CMD [ "adapters.handler.handler" ]
