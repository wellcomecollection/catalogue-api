# Internal model tool

Tooling to help with the versioning of our internal model.


## Installation

```text
virtualenv venv
. venv/bin/activate
pip install -r requirements.txt
```

## Usage

### Bump to the compatible version

```bash
./bump.py
```

### Check compatibility

Check if the `internalModel` in `Dependencies` is compatible with `catalogue-api.mappings._meta`

```bash
./check_compatibility.py
```

