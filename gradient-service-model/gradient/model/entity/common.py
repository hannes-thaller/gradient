import enum
import operator
import typing

import attr
import numpy as np

if typing.TYPE_CHECKING:
    import uuid


class ProjectContext:
    id_project: "uuid.UUID" = attr.ib()
    id_session: "uuid.UUID" = attr.ib()


class NameComponentType(enum.Enum):
    GROUP = 0
    ARTIFACT = 1
    VERSION = 2
    PACKAGE = 3
    TYPE = 4
    PROPERTY = 5
    EXECUTABLE = 6
    PARAMETER = 7
    RESULT = 8


@attr.s(frozen=True, slots=True)
class CanonicalName:
    components: typing.Tuple[str, ...] = attr.ib()
    types: typing.Tuple[NameComponentType, ...] = attr.ib()


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
    name: CanonicalName = attr.ib(default=None)

    def is_number(self) -> bool:
        return self.is_integer() or self.is_long() or self.is_float() or self.is_double()

    def is_boolean(self) -> bool:
        return self.descriptor == DataTypeDescriptor.BOOLEAN

    def is_integer(self) -> bool:
        return self.descriptor == DataTypeDescriptor.INTEGER

    def is_long(self) -> bool:
        return self.descriptor == DataTypeDescriptor.LONG

    def is_float(self) -> bool:
        return self.descriptor == DataTypeDescriptor.FLOAT

    def is_double(self) -> bool:
        return self.descriptor == DataTypeDescriptor.DOUBLE

    def is_string(self) -> bool:
        return self.descriptor == DataTypeDescriptor.STRING


class ModelUniverseStatus(enum.Enum):
    EXTERNAL = 0
    BOUNDARY = 1
    BOUNDARY_MODEL = 2
    INTERNAL = 3
    INTERNAL_MODEL = 4


class FeatureType(enum.Enum):
    CONDITIONAL = 0
    INPUT_PROPERTY = 1
    INPUT_PARAMETER = 2
    INPUT_RESULT = 3
    OUTPUT_PROPERTY = 4
    OUTPUT_PARAMETER = 5
    OUTPUT_RESULT = 6


@attr.s(frozen=True, slots=True)
class Feature:
    element_id: int = attr.ib()
    name: CanonicalName = attr.ib()
    data_type: DataType = attr.ib()
    feature_type: FeatureType = attr.ib()
    alias_ids: typing.Tuple[int, ...] = attr.ib(factory=tuple)
    extend: int = attr.ib(default=1)


@attr.s(frozen=True, slots=True)
class FeatureDescription:
    element_id: int = attr.ib()
    features: typing.Tuple[Feature, ...] = attr.ib()

    @classmethod
    def new(cls, element_id: int, features: typing.List[Feature]):
        features = tuple(sorted(features, key=operator.attrgetter("feature_type", "data_type", "extend")))
        return cls(element_id, features)

    def feature_conditional(self) -> Feature:
        return self.features[0]

    def feature_code_elements(self) -> typing.Tuple[Feature]:
        return self.features[1:]

    def feature_indexes(self) -> typing.List[typing.List[int]]:
        i = 0
        result = []
        for it in self.features:
            result.append(list(range(i, i + it.extend)))
            i += it.extend
        return result

    def feature_splits_code_elements(self) -> typing.List[int]:
        return np.cumsum([it.extend for it in self.features[1:-1]])
