import hypothesis.strategies as s
import numpy as np
import arb
from gradient.domain.inference import transformation
from gradient.domain import api
from hypothesis import given, assume
from hypothesis.extra import numpy as snp

arb_float_small = snp.arrays(np.float32, (10, 5), elements=s.floats(width=16, min_value=-100, max_value=100))
arb_float_int_small = snp.arrays(np.float32, (10, 5), elements=s.integers(min_value=0, max_value=10))


@given(arb.feature, snp.arrays(np.int32, (2, 3)))
def test_data_type_encoder_fit(feature, arr):
    arr = np.ma.masked_invalid(arr)
    sut = transformation.DataTypeTransformation()

    feature = sut.fit(arr, feature)

    assert feature


@given(arb.feature_int, arb_float_small, arb_float_small)
def test_scale_transformation_fit_float32(feature, arr_fit, arr_transform):
    assume(not np.isnan(arr_fit).any())
    assume(not np.isinf(arr_fit).any())

    arr_fit = np.ma.masked_invalid(arr_fit)

    sut = transformation.ScaleTransformation()

    feature = sut.fit(arr_fit, feature)
    arr_z = sut.transform(arr_transform)
    arr_x = sut.inverse(arr_z)

    assert feature
    assert feature.data_type.descriptor == api.DataTypeDescriptor.FLOAT
    assert arr_x.dtype == np.float32
    assert np.allclose(arr_transform[~arr_x.mask], arr_x[~arr_x.mask], atol=1.e-4)


@given(arb.feature_int, snp.arrays(np.int32, (10, 5)), snp.arrays(np.int32, (10, 5)))
def test_ordinal_transformation_fit_int32(feature, arr_fit, arr_transform):
    assume(not np.isnan(arr_fit).any())
    assume(not np.isinf(arr_fit).any())

    arr_fit = np.ma.masked_invalid(arr_fit)

    sut = transformation.OrdinalTransformer()

    feature = sut.fit(arr_fit, feature)
    arr_z = sut.transform(arr_transform)
    arr_x = sut.inverse(arr_z)

    assert feature
    assert feature.data_type.descriptor == api.DataTypeDescriptor.INTEGER
    assert arr_transform.dtype == arr_x.dtype
    assert not any([it not in sut.mapping for it in arr_transform.flatten()]) or (arr_z.mask.any() and arr_x.mask.any())
    assert np.allclose(arr_transform[~arr_x.mask], arr_x[~arr_x.mask])


@given(arb.feature_float, arb_float_int_small, arb_float_int_small)
def test_ordinal_transformation_fit_float32(feature, arr_fit, arr_transform):
    assume(not np.isnan(arr_fit).any())
    assume(not np.isinf(arr_fit).any())

    arr_fit = np.ma.masked_invalid(arr_fit)

    sut = transformation.OrdinalTransformer()

    feature = sut.fit(arr_fit, feature)
    arr_z = sut.transform(arr_transform)
    arr_x = sut.inverse(arr_z)

    assert feature
    assert feature.data_type.descriptor == api.DataTypeDescriptor.INTEGER
    assert arr_transform.dtype == arr_x.dtype
    assert not any([it not in sut.mapping for it in arr_transform.flatten()]) or (arr_z.mask.any() and arr_x.mask.any())
    assert np.allclose(arr_transform[~arr_x.mask], arr_x[~arr_x.mask])


@given(arb.feature_float, arb_float_int_small, arb_float_int_small)
def test_category_transformation_fit_float32(feature, arr_fit, arr_transform):
    assume(not np.isnan(arr_fit).any())
    assume(not np.isinf(arr_fit).any())

    arr_fit = np.ma.masked_invalid(arr_fit)

    sut = transformation.CategoryTransformation()

    feature = sut.fit(arr_fit, feature)
    arr_z = sut.transform(arr_transform)
    arr_x = sut.inverse(arr_z)

    assert feature
    assert feature.data_type.descriptor == api.DataTypeDescriptor.INTEGER
    assert arr_transform.dtype == arr_x.dtype
    assert np.allclose(arr_transform[~arr_x.mask], arr_x[~arr_x.mask])


@given(arb.feature_int,
       snp.arrays(np.int32, (10, 1), elements=s.integers(min_value=0, max_value=32)),
       snp.arrays(np.int32, (10, 1), elements=s.integers(min_value=0, max_value=32)))
def test_base_transformation(feature, arr_fit, arr_transform):
    assume(not np.isnan(arr_fit).any())
    assume(not np.isinf(arr_fit).any())

    arr_fit = np.ma.masked_invalid(arr_fit)

    sut = transformation.BaseNTransformation()

    feature = sut.fit(arr_fit, feature)
    arr_z = sut.transform(arr_transform)
    arr_x = sut.inverse(arr_z)

    assert feature
    assert feature.data_type.descriptor == api.DataTypeDescriptor.INTEGER
    assert arr_transform.dtype == arr_x.dtype
    assert arr_z.shape[1] == arr_transform.shape[1] * feature.extend
    assert not feature.extend < len(np.base_repr(arr_transform.max())) or arr_x.mask.any()
    assert np.allclose(arr_transform[~arr_x.mask], arr_x[~arr_x.mask])


@given(arb.arb_pipeline())
def test_pipeline(inputs):
    features, arr_fit, arr_transform = inputs

    sut = transformation.Pipeline.default()

    features_z = sut.fit(arr_fit, features)
    arr_z = sut.transform(arr_transform)
    arr_x = sut.inverse(arr_z)

    assert len(features) == len(features_z)
    assert all([a.element_id == b.element_id for a, b, in zip(features, features_z)])
    assert all([it.data_type.is_float() for it in features_z])
    assert all([it.dtype == np.float32 for it in arr_z])
    assert all([np.allclose(arr_a[~arr_b.mask], arr_b[~arr_b.mask]) for arr_a, arr_b in zip(arr_transform, arr_x) if arr_a.dtype != np.dtype(np.object)])
    assert all([np.array_equal(arr_a[~arr_b.mask], arr_b[~arr_b.mask]) for arr_a, arr_b in zip(arr_transform, arr_x) if arr_a.dtype == np.dtype(np.object)])
