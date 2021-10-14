import numpy as np
from hypothesis import strategies as s
from hypothesis.extra import numpy as snp

from gradient_inference import entity

_valid_dataset_datatype = {entity.DataTypeDescriptor.BOOLEAN, entity.DataTypeDescriptor.INTEGER,
                           entity.DataTypeDescriptor.LONG, entity.DataTypeDescriptor.FLOAT,
                           entity.DataTypeDescriptor.DOUBLE, entity.DataTypeDescriptor.STRING}


@s.composite
def canonical_name(draw, length=s.integers(3, 7), values=s.text()):
    length = draw(length)
    arb_feature_type = s.sampled_from(entity.NameComponentType)
    components = tuple(draw(values) for _ in range(length))
    types = tuple(draw(arb_feature_type) for _ in range(length))
    return entity.CanonicalName(
        components=components,
        types=types
    )


data_type_descriptor = s.sampled_from(entity.DataTypeDescriptor).filter(lambda x: x in _valid_dataset_datatype)
data_type_descriptor_int = (s.sampled_from(entity.DataTypeDescriptor)
                            .filter(lambda x: x == entity.DataTypeDescriptor.INTEGER))
data_type_descriptor_float = (s.sampled_from(entity.DataTypeDescriptor)
                              .filter(lambda x: x == entity.DataTypeDescriptor.FLOAT))

data_type = s.builds(entity.DataType, data_type_descriptor, canonical_name())
data_type_singular = s.builds(entity.DataType, data_type_descriptor, canonical_name())
data_type_int = s.builds(entity.DataType, data_type_descriptor_int, canonical_name())
data_type_float = s.builds(entity.DataType, data_type_descriptor_float, canonical_name())

feature = s.builds(entity.Feature, s.integers(0), canonical_name(), data_type,
                   s.sampled_from(entity.FeatureType), s.lists(s.integers(), max_size=4))
feature_int = s.builds(entity.Feature, s.integers(0), canonical_name(), data_type_int,
                       s.sampled_from(entity.FeatureType), s.lists(s.integers(), max_size=4))
feature_float = s.builds(entity.Feature, s.integers(0), canonical_name(), data_type_float,
                         s.sampled_from(entity.FeatureType), s.lists(s.integers(), max_size=4))


@s.composite
def dataset(draw, n, m):
    return draw(snp.arrays(np.float32, (n, m), elements=s.floats(min_value=-1, max_value=1, width=16, allow_nan=False, allow_infinity=False)))


@s.composite
def arb_pipeline(draw):
    dtype_map = {
        entity.DataTypeDescriptor.BOOLEAN: np.dtype(np.bool),
        entity.DataTypeDescriptor.FLOAT: np.dtype(np.float32),
        entity.DataTypeDescriptor.DOUBLE: np.dtype(np.float64),
        entity.DataTypeDescriptor.INTEGER: np.dtype(np.int32),
        entity.DataTypeDescriptor.LONG: np.dtype(np.int64),
        entity.DataTypeDescriptor.STRING: np.dtype(np.str),
    }
    elements_map = {
        entity.DataTypeDescriptor.BOOLEAN: s.booleans(),
        entity.DataTypeDescriptor.FLOAT: s.floats(width=16, allow_nan=False, allow_infinity=False),
        entity.DataTypeDescriptor.DOUBLE: s.floats(width=16, allow_nan=False, allow_infinity=False),
        entity.DataTypeDescriptor.INTEGER: s.integers(min_value=1000, max_value=1000),
        entity.DataTypeDescriptor.LONG: s.integers(min_value=1000, max_value=1000),
        entity.DataTypeDescriptor.STRING: None
    }
    arb_feature_cond = s.builds(entity.Feature, s.integers(0), canonical_name(), data_type_int,
                                s.sampled_from([entity.FeatureType.CONDITIONAL]), s.lists(s.integers(), max_size=4))
    arb_feature_non_cond = s.builds(entity.Feature, s.integers(0), canonical_name(), data_type,
                                    s.sampled_from(entity.FeatureType).filter(
                                        lambda x: x != entity.FeatureType.CONDITIONAL),
                                    s.lists(s.integers(), max_size=4))

    arb_features_non_cond = s.lists(arb_feature_non_cond, min_size=1, max_size=5)
    features = [draw(arb_feature_cond)] + draw(arb_features_non_cond)

    def draw_ars():
        arb_arr_fit = [snp.arrays(dtype_map[it.data_type.descriptor], (20, 1), elements=elements_map[it.data_type.descriptor]) for it in features]
        arrs = [draw(it) for it in arb_arr_fit]
        return [it.astype(np.object) if it.dtype.kind in {"U", "S"} else it for it in arrs]

    return features, draw_ars(), draw_ars()
