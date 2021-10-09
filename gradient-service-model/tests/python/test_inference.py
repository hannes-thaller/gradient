from datetime import timedelta

import attr
import numpy as np
from gradient.domain import api, entity
import arb
from gradient.domain.inference import inference
from hypothesis import strategies as s, given, settings
from torch import random

seed = 15


@settings(deadline=timedelta(milliseconds=1000))
@given(arb.dataset(n=50, m=10), s.lists(arb.feature_float, min_size=10, max_size=10))
def test_nvp_fit(dataset, features):
    np.random.seed(seed)
    random.manual_seed(seed)

    arrs = [dataset[:, [i]] for i in range(dataset.shape[1])]

    features = [attr.evolve(features[0], feature_type=api.FeatureType.CONDITIONAL)] + [attr.evolve(it, feature_type=api.FeatureType.INPUT_PARAMETER) for it in features[1:]]
    feature_descriptions = api.FeatureDescription(0, tuple(features))
    nvp = inference.NvpKernel()
    report = nvp.fit(arrs, feature_descriptions, entity.HyperParameters(max_epoch=10))

    nvp_result = nvp.sample(50)
    x = np.ma.masked_invalid(np.concatenate(nvp_result.x, 1))
    training_losses = [it["loss"] for it in report.loss_record if "training" in it["tags"]]
    mask = ~x.mask.any(1)

    assert len(report.loss_record) > 0
    assert training_losses[0] > training_losses[-1]
    assert np.allclose(np.median(dataset[mask], axis=1), np.median(x[mask], axis=1), atol=1e-2)
