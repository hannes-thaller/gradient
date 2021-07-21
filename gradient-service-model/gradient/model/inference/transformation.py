import logging
import typing

import attr
import bidict
import itertools
import numpy as np

from gradient.model.entity import common

_logger = logging.getLogger(__name__)

_dtype_to_data_type = {
    np.dtype(np.int32): common.DataTypeDescriptor.INTEGER,
    np.dtype(np.int64): common.DataTypeDescriptor.LONG,
    np.dtype(np.bool): common.DataTypeDescriptor.BOOLEAN,
    np.dtype(np.float32): common.DataTypeDescriptor.FLOAT,
    np.dtype(np.float64): common.DataTypeDescriptor.DOUBLE,
    np.dtype(np.object): common.DataTypeDescriptor.STRING,
}


class Transformation:
    def fit(self, arr: np.ma.masked_array, feature: common.Feature) -> common.Feature:
        assert not arr.mask.any()
        return feature

    def transform(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        return arr

    def inverse(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        return arr


@attr.s(slots=True)
class DataTypeTransformation(Transformation):
    dtype_transform: np.dtype = attr.ib(default=None)
    dtype_inverse: np.dtype = attr.ib(default=None)

    def fit(self, arr: np.ma.masked_array, feature: common.Feature) -> common.Feature:
        assert arr.dtype in _dtype_to_data_type
        assert not arr.mask.any()
        if self.dtype_transform is None:
            self.dtype_transform = arr.dtype
        if self.dtype_inverse is None:
            self.dtype_inverse = arr.dtype

        data_type = attr.evolve(feature.data_type, descriptor=_dtype_to_data_type[self.dtype_transform])
        return attr.evolve(feature, data_type=data_type)

    def transform(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        assert self.dtype_transform
        if arr.dtype != self.dtype_transform:
            arr = arr.astype(self.dtype_transform)

        assert np.ma.isMaskedArray(arr)
        return arr

    def inverse(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        assert self.dtype_inverse
        if arr.dtype != self.dtype_inverse:
            arr = arr.astype(self.dtype_inverse)

        assert np.ma.isMaskedArray(arr)
        return arr


@attr.s(slots=True)
class ScaleTransformation(Transformation):
    mean: np.array = attr.ib(init=False, default=None)
    scale: np.array = attr.ib(init=False, default=None)

    def fit(self, arr: np.ma.masked_array, feature: common.Feature) -> common.Feature:
        assert arr is not None and arr.dtype == np.float32
        assert not np.isnan(arr).any() and not np.isinf(np.abs(arr)).any()
        assert self.mean is None
        assert self.scale is None
        assert not arr.mask.any()
        assert feature.data_type.descriptor.is_number()

        data_type = common.DataType(common.DataTypeDescriptor.FLOAT)
        self.mean = np.array(arr.mean(axis=0, dtype=np.float32))
        self.mean[np.isinf(self.mean)] = 0.

        self.scale = np.array(arr.std(axis=0, dtype=np.float32))
        self.scale[self.scale == 0] = 1.
        self.scale[np.isinf(self.scale)] = 1.
        # capture zero variance data

        assert not np.isnan(self.mean).any() and not np.isinf(np.abs(self.mean)).any()
        assert not np.isnan(self.scale).any() and not np.isinf(np.abs(self.scale)).any()

        return attr.evolve(feature, data_type=data_type)

    def transform(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        assert arr.ndim == 2
        assert arr.shape[1] == self.mean.shape[0]
        return np.ma.masked_invalid(np.divide(arr - self.mean, self.scale, dtype=np.float32))

    def inverse(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        assert arr.ndim == 2
        assert arr.shape[1] == self.mean.shape[0]
        return np.ma.masked_invalid(arr * self.scale + self.mean).astype(np.float32)


@attr.s(slots=True)
class OrdinalTransformer(Transformation):
    values: typing.List = attr.ib(default=None)
    mapping: bidict = attr.ib(init=False)
    dtype_input: np.dtype = attr.ib(init=False)
    dtype_output: np.dtype = attr.ib(init=False, default=np.dtype(np.int32))

    def fit(self, arr: np.ma.masked_array, feature: common.Feature) -> common.Feature:
        assert arr is not None
        assert not arr.mask.any()
        values = self.values if self.values is not None else np.unique(arr).tolist()
        self.mapping = bidict.frozenbidict({value: encoding for encoding, value in enumerate(values)})
        self.dtype_input = arr.dtype
        return attr.evolve(feature, data_type=common.DataType(_dtype_to_data_type[self.dtype_output]))

    @staticmethod
    def _lookup_forward(mapping, it):
        return mapping[it] if it is not np.ma.masked and it in mapping else np.ma.masked

    @staticmethod
    def _lookup_backward(mapping, it):
        return mapping.inverse[it] if it is not np.ma.masked and it in mapping.inverse else np.ma.masked

    def transform(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        assert arr.ndim == 2
        return (np.ma.array([np.ma.array([self._lookup_forward(self.mapping, it) for it in row]) for row in arr])
                .astype(self.dtype_output))

    def inverse(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        assert arr.ndim == 2
        return (np.ma.array([np.ma.array([self._lookup_backward(self.mapping, it) for it in row]) for row in arr])
                .astype(self.dtype_input))


@attr.s(slots=True)
class CategoryTransformation(Transformation):
    max_levels: int = attr.ib(default=16)
    transformation_ordinal: OrdinalTransformer = attr.ib(default=None)

    def fit(self, arr: np.ma.masked_array, feature: common.Feature) -> common.Feature:
        assert arr is not None
        assert not arr.mask.any()

        levels = np.unique(arr)
        if levels.shape[0] <= self.max_levels:
            self.transformation_ordinal = OrdinalTransformer()
            result = self.transformation_ordinal.fit(arr, feature)
        else:
            result = feature

        return result

    def transform(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        if self.transformation_ordinal:
            return self.transformation_ordinal.transform(arr)
        else:
            return arr

    def inverse(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        if self.transformation_ordinal:
            return self.transformation_ordinal.inverse(arr)
        else:
            return arr


@attr.s(slots=True)
class BaseNTransformation(Transformation):
    """
    TODO move discretization into separate stage and add the previous transformations as argument to the fit method.
    Can cause nans if the value of the input is larger then under fitting by a factor of <base>.
    """
    base: int = attr.ib(default=2)
    extend: int = attr.ib(default=0, init=False)
    mapping: bidict = attr.ib(default=None, init=False)
    _encodings: np.ndarray = attr.ib(default=None, init=False)

    def fit(self, arr: np.ma.masked_array, feature: common.Feature) -> common.Feature:
        assert arr is not None and arr.dtype == np.int32
        assert arr.ndim == 2

        self.extend = len(np.base_repr(arr.max(), self.base))
        self.mapping = bidict.frozenbidict({it: tuple(self.to_digits(it, self.base, self.extend))
                                            for it in range(arr.max() + 1)})
        data_type = common.DataType(common.DataTypeDescriptor.INTEGER)
        return attr.evolve(feature, data_type=data_type, extend=self.extend)

    def _lookup_forward(self, it):
        return self.mapping[it] if it is not np.ma.masked and it in self.mapping else [np.ma.masked] * self.extend

    def _lookup_backward(self, it):
        return self.mapping.inverse.get(tuple(it), np.ma.masked) if not it.mask.any() else np.ma.masked

    def _discretize_encoding(self, arr):
        """
        Compute mean absoulte error to find most likely code given a floating point number.
        If the error is larger then 1 (step size of the ordinal) then there is no likely value and it should be masked.
        """
        if self._encodings is None:
            self._encodings = np.atleast_2d(np.array(list(self.mapping.values())))

        mask = arr.mask
        arr = arr[:, np.newaxis]
        code_mae = np.abs(self._encodings - arr[:, ]).mean(axis=2)
        code_idx = code_mae.argmin(axis=1)
        mask |= (code_mae.min(1) > 1)[:, np.newaxis]
        return np.ma.masked_array(self._encodings[code_idx], mask=mask)

    def transform(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        assert arr.ndim == 2 and arr.shape[1] == 1
        assert arr.dtype == np.int32
        return np.ma.masked_invalid([np.ma.masked_invalid(self._lookup_forward(it[0])) for it in arr]).astype(np.int32)

    def inverse(self, arr: np.ma.masked_array, **kwargs) -> np.ma.masked_array:
        assert arr is not None
        assert arr.ndim == 2 and arr.shape[1] == self.extend
        arr_discrete = self._discretize_encoding(arr)
        return np.ma.masked_invalid([np.ma.masked_invalid([self._lookup_backward(it)]) for it in arr_discrete]).astype(np.int32)

    @staticmethod
    def to_digits(number, base, min_digits):
        digits = []
        while number > 0:
            digits.append(number % base)
            number = number // base
        digits.reverse()
        return list(itertools.chain(itertools.repeat(0, min_digits - len(digits)), digits))


@attr.s(slots=True)
class Pipeline:
    _transformation_factories: typing.List[typing.Callable[[common.Feature, typing.List[Transformation]], Transformation]] = attr.ib()
    _features: typing.List[typing.List[common.Feature]] = attr.ib(factory=list, init=False)
    _transformations: typing.List[typing.List[Transformation]] = attr.ib(factory=list, init=False)
    feature_description_input: common.FeatureDescription = attr.ib(init=False)
    feature_description_output: common.FeatureDescription = attr.ib(init=False)

    @classmethod
    def default(cls):
        def has_prev_categorization(ts):
            return any([it.__class__ == OrdinalTransformer or it.__class__ == CategoryTransformation for it in ts])

        factories = [lambda _, __: DataTypeTransformation(),
                     lambda f, _: OrdinalTransformer([False, True]) if f.data_type.is_boolean() else Transformation(),
                     lambda f, _: OrdinalTransformer() if f.data_type.is_string() else Transformation(),
                     lambda f, ts: CategoryTransformation() if f.data_type.is_number() and not has_prev_categorization(ts) else Transformation(),
                     lambda f, ts: BaseNTransformation(10) if has_prev_categorization(ts) else Transformation(),
                     lambda _, __: DataTypeTransformation(dtype_transform=np.dtype(np.float32), dtype_inverse=np.dtype(np.float32)),
                     lambda f, __: ScaleTransformation()]
        return cls(factories)

    @staticmethod
    def _to_masked(arrs):
        return [np.ma.masked_array(it) if it.dtype == np.dtype(np.object) else np.ma.masked_invalid(it) for it in arrs]

    def fit(self, arrs: typing.List[np.array],
            feature_description: common.FeatureDescription) -> common.FeatureDescription:
        assert arrs
        assert len(arrs) == len(feature_description.features)
        assert all([it.ndim == 2 for it in arrs])
        assert all([arr.shape[1] == f.extend for arr, f in zip(arrs, feature_description.features)])

        data = self._to_masked(arrs)
        features = feature_description.features
        for factory in self._transformation_factories:
            transformations = []
            features_next = []
            data_next = []

            for i, (feature, arr_feature) in enumerate(zip(features, data)):
                transformation = factory(feature, [it[i] for it in self._transformations])
                transformations.append(transformation)
                features_next.append(transformation.fit(arr_feature, feature))
                data_next.append(transformation.transform(arr_feature))

            if not all([it.__class__ == Transformation for it in transformations]):
                data = data_next
                features = features_next
                self._transformations.append(transformations)
                self._features.append(features_next)

        self.feature_description_input = feature_description
        self.feature_description_output = common.FeatureDescription(feature_description.element_id, features)

        return self.feature_description_output

    def transform(self, arrs: typing.List[np.array]) -> typing.List[np.ma.masked_array]:
        assert arrs and self._features
        assert len(arrs) == len(self.feature_description_input.features)
        assert all([it.ndim == 2 for it in arrs])
        assert all([arr.shape[1] == f.extend for arr, f in zip(arrs, self._features[0])])

        data = self._to_masked(arrs)
        for li, layer in enumerate(self._transformations):
            data_next = []
            for fi, (transformation, arr) in enumerate(zip(layer, data)):
                arr_transformed = transformation.transform(arr)
                data_next.append(arr_transformed)
                if arr_transformed.mask.any():
                    _logger.warning(
                        f"Transformation (forward) yielded data with invalid values: level={li}, feature={fi}, transformation={transformation}, invalid_sum={arr_transformed.mask.sum()}")

            data = data_next

        assert all([np.ma.isMaskedArray(it) for it in data])
        return data

    def inverse(self, arrs: typing.List[np.array]) -> typing.List[np.ma.masked_array]:
        assert arrs and self._features
        assert len(arrs) == len(self.feature_description_output.features)
        assert all([it.ndim == 2 for it in arrs])
        assert all([arr.shape[1] == f.extend for arr, f in zip(arrs, self._features[-1])])

        warned = False
        data = self._to_masked(arrs)
        for li, layer in enumerate(reversed(self._transformations)):
            data_next = []
            for fi, (transformation, arr) in enumerate(zip(layer, data)):
                arr_transformed = transformation.inverse(arr)
                data_next.append(arr_transformed)
                if not warned and arr_transformed.mask.any():
                    warned = True
                    _logger.warning(
                        f"Transformation (inverse) yielded data with invalid values: level={li}, feature={fi}, transformation={transformation}, invalid_sum={arr_transformed.mask.sum()}")

            data = data_next

        assert all([np.ma.isMaskedArray(it) for it in data])
        return data

    def transform_feature(self, arr: np.ndarray, feature: common.Feature) -> np.ma.masked_array:
        assert arr is not None and self._features
        assert feature in self.feature_description_input.features, "Given feature is not an input feature"
        assert arr.ndim == 2
        assert arr.shape[1] == feature.extend
        assert np.dtype(arr.dtype) in _dtype_to_data_type

        warned = False
        data = np.ma.masked_array(arr) if arr.dtype == np.dtype(np.object) else np.ma.masked_invalid(arr)

        fi = self.feature_description_input.features.index(feature)
        for li, layer in enumerate(self._transformations):
            transformation = layer[fi]
            data = transformation.transform(data)

            if not warned and data.mask.any():
                warned = True
                _logger.warning(
                    f"Transformation (inverse) yielded data with invalid values: level={li}, feature={fi}, transformation={transformation}, invalid_sum={data.mask.sum()}")

        assert np.ma.isMaskedArray(data)
        return data

    def inverse_feature(self, arr: np.ndarray, feature: common.Feature) -> np.ma.masked_array:
        assert arr and self._features
        assert feature in self.feature_description_output.features, "Given feature is not an output feature"
        assert arr.ndim == 2
        assert arr.shape[1] == feature.extend
        assert np.dtype(arr.dtype) in _dtype_to_data_type

        data = np.ma.masked_array(arr) if arr.dtype == np.dtype(np.object) else np.ma.masked_invalid(arr)

        fi = self.feature_description_output.features.index(feature)
        for li, layer in enumerate(reversed(self._transformations)):
            transformation = layer[fi]
            data = transformation.inverse(data)

            if data.mask.any():
                _logger.warning(
                    f"Transformation (inverse) yielded data with invalid values: level={li}, feature={fi}, transformation={transformation}, invalid_sum={data.mask.sum()}")

        assert np.ma.isMaskedArray(data)
        return data
