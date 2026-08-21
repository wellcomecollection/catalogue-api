"""Input validation for the Identifiers API.

Pure, dependency-free. In production these checks run at the API Gateway (a
regex on the path parameter, an enum on the query parameter) so malformed
requests are rejected with a 400 before reaching the Lambda. The prototype has
no gateway, so the same rules live here and the handler applies them.
"""

import re

# 8-char public catalogue id: alphabet a-z and 2-9, excluding o/i/l/1, first
# character a letter. Matches the gateway regex in the contract.
CANONICAL_ID_PATTERN = re.compile(r"^[a-hjkmnp-z][a-hjkmnp-z2-9]{7}$")

# Ontology type is a real key component, not decoration. `Item` is required by
# the requesting use case (RFC 088).
VALID_TYPES = ("Work", "Image", "Item")
DEFAULT_TYPE = "Work"

# The only documented value of the reverse-lookup `include` parameter.
VALID_INCLUDE = ("siblings",)


def is_valid_canonical_id(value: str) -> bool:
    return bool(CANONICAL_ID_PATTERN.match(value or ""))


def is_valid_type(value: str) -> bool:
    return value in VALID_TYPES


def is_valid_include(value: str) -> bool:
    return value in VALID_INCLUDE
