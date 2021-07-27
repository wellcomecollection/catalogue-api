#!/usr/bin/env python
"""
This script checks the local version against what's been set
as the latest version in in the catalogue-api `mappings._meta`.pwd
"""

from common import (
    get_session,
    get_local_date,
    get_local_internal_model,
    get_remote_meta,
)


if __name__ == "__main__":
    session = get_session(role_arn="arn:aws:iam::756629837203:role/catalogue-ci")
    date = get_local_date()
    internal_model = get_local_internal_model()
    version, hash = internal_model.split(".")
    meta = get_remote_meta(session, date)
    meta_key = f"model.versions.{version}"
    is_compatible = meta_key in meta and meta[meta_key] == hash
    if is_compatible:
        print(f"{meta_key} is compatible with {meta}")
        exit(0)
    else:
        print(f"{meta_key} is incompatible with {meta}")
        exit(1)
