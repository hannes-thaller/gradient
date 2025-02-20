import enum

import attr
import typing
import uuid

from . import common


class DataType(enum.Enum):
    BOOLEAN = 1
    INTEGER = 2
    FLOAT = 3
    STRING = 4


@attr.s(frozen=True, slots=True)
class Variable:
    id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    type_data: DataType = attr.ib()


@attr.s(frozen=True, slots=True)
class Factor:
    id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    variables: typing.Tuple[int, ...] = attr.ib(factory=tuple)


@attr.s(frozen=True, slots=True)
class Cluster:
    id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    variables: typing.Tuple[int, ...] = attr.ib(factory=tuple)


@attr.s(frozen=True, slots=True)
class Sepset:
    id_cluster_left: int = attr.ib()
    id_cluster_right: int = attr.ib()
    variable_map: typing.Dict[int, int] = attr.ib(factory=dict)


@attr.s(frozen=True, slots=True)
class FactorGraph:
    id: uuid.UUID = attr.ib()
    factors: typing.Tuple[Factor, ...] = attr.ib(factory=tuple)
    variables: typing.Tuple[Variable, ...] = attr.ib(factory=tuple)


@attr.s(frozen=True, slots=True)
class ClusterGraph:
    id: uuid.UUID = attr.ib()
    clusters: typing.Tuple[Cluster, ...] = attr.ib(factory=tuple)
    variables: typing.Tuple[Variable, ...] = attr.ib(factory=tuple)
    sepsets: typing.Tuple[Sepset, ...] = attr.ib(factory=tuple)
