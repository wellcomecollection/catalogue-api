"""GET /identifiers/v1/source-systems: contract, ordering and caching."""

from conftest import AssertContract, Invoke, body

from adapters.run_local import _route
from core.source_systems import SOURCE_SYSTEMS
from core.validation import VALID_TYPES

SOURCE_SYSTEMS_PATH = "/identifiers/v1/source-systems"


def test_200_matches_source_system_list(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(SOURCE_SYSTEMS_PATH)
    assert_contract(result, "GET", SOURCE_SYSTEMS_PATH, 200)


def test_ids_are_unique_and_sorted(invoke: Invoke) -> None:
    ids = [s["id"] for s in body(invoke(SOURCE_SYSTEMS_PATH))["results"]]
    assert len(ids) == len(set(ids))
    assert ids == sorted(ids)


def test_types_are_valid_and_in_canonical_order(invoke: Invoke) -> None:
    for source_system in body(invoke(SOURCE_SYSTEMS_PATH))["results"]:
        types = source_system["types"]
        assert types == [t for t in VALID_TYPES if t in types]


def test_data_draws_types_only_from_valid_types() -> None:
    for source_system in SOURCE_SYSTEMS:
        assert source_system.types
        assert set(source_system.types) <= set(VALID_TYPES), source_system.id


def test_emits_cache_control_and_strong_etag(invoke: Invoke) -> None:
    headers = invoke(SOURCE_SYSTEMS_PATH)["headers"]
    assert headers["Cache-Control"] == "public, max-age=86400"
    etag = headers["ETag"]
    assert etag.startswith('"') and etag.endswith('"')


def test_if_none_match_returns_304(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    etag = invoke(SOURCE_SYSTEMS_PATH)["headers"]["ETag"]
    result = invoke(SOURCE_SYSTEMS_PATH, headers={"If-None-Match": etag})
    assert result["statusCode"] == 304
    assert result["headers"]["ETag"] == etag
    assert result["body"] == ""
    assert_contract(result, "GET", SOURCE_SYSTEMS_PATH, 304)


def test_local_invoker_routes_literal_path_before_canonical_id() -> None:
    assert _route(SOURCE_SYSTEMS_PATH) == (SOURCE_SYSTEMS_PATH, {})
