import enum

import attr
import itertools
import typing

if typing.TYPE_CHECKING:
    from . import common


class DataTypeDescriptor(enum.Enum):
    NONE = 0
    BOOLEAN = 1
    BOOLEANS = 2
    INTEGER = 3
    INTEGERS = 4
    LONG = 5
    LONGS = 6
    FLOAT = 7
    FLOATS = 8
    DOUBLE = 9
    DOUBLES = 10
    STRING = 11
    STRINGS = 12
    REFERENCE = 13
    REFERENCES = 14

    def is_number(self) -> bool:
        return (self == DataTypeDescriptor.INTEGER or
                self == DataTypeDescriptor.LONG or
                self == DataTypeDescriptor.FLOAT or
                self == DataTypeDescriptor.DOUBLE)


@attr.s(frozen=True, slots=True)
class DataType:
    descriptor: DataTypeDescriptor = attr.ib()
    name: "common.CanonicalName" = attr.ib(default=None)


@attr.s(frozen=True, slots=True, cache_hash=True)
class Type:
    id: int = attr.ib()
    name: "common.CanonicalName" = attr.ib()
    status: "ModelUniverseStatus" = attr.ib()
    properties: typing.Tuple[int, ...] = attr.ib()
    executables: typing.Tuple[int, ...] = attr.ib()

    def child_relationship_ids(self) -> typing.Iterable[int]:
        yield itertools.chain(
            self.properties,
            self.executables
        )


@attr.s(frozen=True, slots=True, cache_hash=True)
class Property:
    id: int = attr.ib()
    name: "common.CanonicalName" = attr.ib()
    status: "ModelUniverseStatus" = attr.ib()
    type_data: "DataType" = attr.ib()
    is_class_member: bool = attr.ib()
    is_immutable: bool = attr.ib()


@attr.s(frozen=True, slots=True, cache_hash=True)
class Executable:
    id: int = attr.ib()
    name: "common.CanonicalName" = attr.ib()
    status: "ModelUniverseStatus" = attr.ib()
    type_data: "DataType" = attr.ib()
    is_class_member: bool = attr.ib()
    is_abstract: bool = attr.ib()
    is_constructor: bool = attr.ib()
    reads: typing.Tuple[int, ...] = attr.ib()
    writes: typing.Tuple[int, ...] = attr.ib()
    receives: typing.Tuple[int, ...] = attr.ib()
    returns: typing.Tuple[int, ...] = attr.ib()
    provides: typing.Tuple[int, ...] = attr.ib()
    requests: typing.Tuple[int, ...] = attr.ib()

    def child_relationship_ids(self) -> typing.Iterable[int]:
        yield itertools.chain(
            self.reads,
            self.writes,
            self.receives,
            self.returns,
            self.provides,
            self.requests
        )


@attr.s(frozen=True, slots=True, cache_hash=True)
class Parameter:
    id: int = attr.ib()
    name: "common.CanonicalName" = attr.ib()
    status: "ModelUniverseStatus" = attr.ib()
    type_data: "DataType" = attr.ib()
    index: int = attr.ib()


@attr.s(frozen=True, slots=True, cache_hash=True)
class Result:
    id: int = attr.ib()
    name: "common.CanonicalName" = attr.ib()
    status: "ModelUniverseStatus" = attr.ib()
    type_data: "DataType" = attr.ib()


class ModelUniverseStatus(enum.Enum):
    EXTERNAL = 0
    BOUNDARY = 1
    BOUNDARY_MODEL = 2
    INTERNAL = 3
    INTERNAL_MODEL = 4


@attr.s(frozen=True, slots=True, cache_hash=True)
class StructureGraph:
    types: typing.Tuple[Type] = attr.ib(factory=tuple)
    properties: typing.Tuple[Property] = attr.ib(factory=tuple)
    executables: typing.Tuple[Executable] = attr.ib(factory=tuple)
    parameters: typing.Tuple[Parameter] = attr.ib(factory=tuple)
    results: typing.Tuple[Result] = attr.ib(factory=tuple)

    def __iter__(self) -> typing.Iterable["CodeElement"]:
        yield itertools.chain(
            self.types,
            self.properties,
            self.executables,
            self.parameters,
            self.results
        )

    def atomic_elements(self) -> typing.Iterable["AtomicElement"]:
        yield itertools.chain(
            self.properties,
            self.parameters,
            self.results
        )

    def compositional_elements(self) -> typing.Iterable["CompositionalElement"]:
        yield itertools.chain(
            self.types,
            self.executables
        )


CodeElement = typing.Union[Type, Property, Executable, Parameter, Result]
CompositionalElement = typing.Union[Type, Executable]
AtomicElement = typing.Union[Property, Parameter, Result]
