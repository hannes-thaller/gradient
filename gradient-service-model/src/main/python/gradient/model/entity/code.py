import typing

import attr

if typing.TYPE_CHECKING:
    from . import common


@attr.s(frozen=True, slots=True, cache_hash=True)
class Type:
    id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    status: common.ModelUniverseStatus = attr.ib()


@attr.s(frozen=True, slots=True, cache_hash=True)
class Property:
    id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    status: common.ModelUniverseStatus = attr.ib()
    type_data: "common.DataType" = attr.ib()


@attr.s(frozen=True, slots=True, cache_hash=True)
class Executable:
    id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    status: common.ModelUniverseStatus = attr.ib()
    type_data: "common.DataType" = attr.ib()


@attr.s(frozen=True, slots=True, cache_hash=True)
class Parameter:
    id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    status: common.ModelUniverseStatus = attr.ib()
    type_data: "common.DataType" = attr.ib()
    index: int = attr.ib()


@attr.s(frozen=True, slots=True, cache_hash=True)
class Result:
    id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    status: common.ModelUniverseStatus = attr.ib()
    type_data: "common.DataType" = attr.ib()
