import enum

import attr
import operator
import typing

from . import common


class FeatureType(enum.Enum):
    INPUT_PROPERTY = 1
    INPUT_PARAMETER = 2
    INPUT_RESULT = 3
    OUTPUT_PROPERTY = 4
    OUTPUT_PARAMETER = 5
    OUTPUT_RESULT = 6


@attr.s(frozen=True, slots=True)
class Feature:
    element_id: int = attr.ib()
    name: common.CanonicalName = attr.ib()
    data_type: common.DataType = attr.ib()
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
