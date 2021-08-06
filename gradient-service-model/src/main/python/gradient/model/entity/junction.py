import enum
import typing

import attr

from . import common

if typing.TYPE_CHECKING:
    from .. import inference


class ModelElementType(enum.Enum):
    EXECUTABLE = 0
    PROPERTY = 1
    PARAMETER = 2
    RESULT = 3


@attr.s(frozen=True, slots=True, cache_hash=True)
class ModelElement:
    id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    type_element: ModelElementType = attr.ib()
    type_data: common.DataType = attr.ib()


@attr.s(frozen=True, slots=True)
class Variable:
    model_element: ModelElement = attr.ib()


@attr.s(frozen=True, slots=True)
class Factor:
    model_element: ModelElement = attr.ib()
    kernel: "inference.Kernel" = attr.ib()


@attr.s(frozen=True, slots=True)
class SuperFactor:
    factors: typing.FrozenSet[Factor] = attr.ib()


@attr.s(frozen=True, slots=True)
class SeparationSet:
    factors: typing.FrozenSet[Factor] = attr.ib()
