#!/usr/bin/env python
"""
This script updates the version of internal_model in Dependencies.scala
to the latest version in in the catalogue-api `mappings._meta`.

This saves somebody having to look up what the exact version/commit ID is.
"""

from common import (
    get_session,
    get_local_date,
    get_remote_latest_internal_model,
    set_local_internal_model,
)

if __name__ == "__main__":
    session = get_session(role_arn="arn:aws:iam::760097843905:role/platform-read_only")
    catalogue_session = get_session(
        role_arn="arn:aws:iam::756629837203:role/catalogue-developer"
    )
    date = get_local_date()
    latest_internal_model = get_remote_latest_internal_model(catalogue_session, date)
    set_local_internal_model(latest_internal_model)
    print(f"set dependency to {set_local_internal_model}")
