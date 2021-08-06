import logging
import typing
import uuid

import attr
import math
import numpy as np
import torch
from torch import nn

from src.main.python.gradient.model.entity import common
from src.main.python.gradient.model import util
from src.main.python.gradient.model.inference import transformation, optimization

_logger = logging.getLogger(__name__)


def _mask_checkboard(shape, grouping=None) -> np.ndarray:
    assert shape
    if grouping is None:
        grouping = [[it] for it in range(shape[1])]

    mask_template = np.array([[it in ones for it in range(shape[1])] for ones in grouping],
                             dtype=np.int)
    grouping_idx = [[j for j in range(len(grouping)) if j % 2 == (i % 2)]
                    for i in range(shape[0])]
    mask = np.vstack([mask_template[it, :].sum(axis=0) for it in grouping_idx])
    return mask


class Gelu(nn.Module):
    __constants__ = ["beta"]

    def __init__(self):
        super().__init__()
        self.beta = math.sqrt(2 / math.pi)

    def forward(self, x):
        return 0.5 * x * (1 + torch.tanh(self.beta * (x + 0.044715 * torch.pow(x, 3))))


class TranslationNetwork(nn.Module):
    __constants__ = ["network"]

    def __init__(self, dim_x: int, dim_c: int):
        super().__init__()
        self.network = nn.Sequential(
            nn.Linear(dim_x + dim_c, 128),
            Gelu(),
            nn.Linear(128, 128),
            Gelu(),
            nn.Linear(128, dim_x)
        )

    def forward(self, x: torch.Tensor):
        return self.network(x)


class ScaleNetwork(nn.Module):
    __constants__ = ["network"]

    def __init__(self, dim_x: int, dim_c: int):
        super().__init__()
        self.network = nn.Sequential(
            nn.Linear(dim_x + dim_c, 128),
            Gelu(),
            nn.Linear(128, 128),
            Gelu(),
            nn.Linear(128, dim_x),
            nn.Tanh()
        )

    def forward(self, x: torch.Tensor):
        return self.network(x)


class CouplingNetwork(nn.Module):
    __constants__ = ["translation", "log_scale"]

    def __init__(self, translation: nn.ModuleList, log_scale: nn.ModuleList, mask: np.ndarray):
        super().__init__()
        self.mask: torch.Tensor = torch.from_numpy(mask)
        self.mask_inverse: torch.Tensor = torch.ones(1) - self.mask
        self.translation = translation
        self.log_scale = log_scale

    def cpu(self):
        self.mask = self.mask.cpu()
        self.mask_inverse = self.mask_inverse.cpu()
        self.translation.cpu()
        self.log_scale.cpu()
        return super().cpu()

    def cuda(self, device=None):
        self.mask = self.mask.cuda(device)
        self.mask_inverse = self.mask_inverse.cuda(device)
        self.translation.cuda(device)
        self.log_scale.cuda(device)
        return super().cuda(device)

    def forward(self, x: torch.Tensor, c: torch.Tensor):
        x1 = x * self.mask

        x1_c = torch.cat([c, x1], dim=1)
        log_scale = self.log_scale(x1_c) * self.mask_inverse
        translation = self.translation(x1_c) * self.mask_inverse

        x2 = self.mask_inverse * (x * torch.exp(log_scale) + translation)

        y = x1 + x2
        log_det_jacobian = log_scale.sum(dim=1)

        return y, log_det_jacobian

    def inverse(self, y: torch.Tensor, c: torch.Tensor):
        y1 = y * self.mask

        y1_c = torch.cat([c, y1], dim=1)
        log_scale: torch.Tensor = self.log_scale(y1_c) * self.mask_inverse
        translation = self.translation(y1_c) * self.mask_inverse

        y2 = self.mask_inverse * (y - translation) * torch.exp(-log_scale)

        x = y1 + y2

        log_det_jacobian = -log_scale.sum(dim=1)

        return x, log_det_jacobian


class Nvp(nn.Module):
    __constants__ = ["couplings", "couplings_reverse", "dim_x"]

    def __init__(self, count_couplings: int, dim_x: int, dim_c: int, mask: np.ndarray):
        super().__init__()
        self.dim_x: int = dim_x
        couplings = [CouplingNetwork(TranslationNetwork(dim_x, dim_c), ScaleNetwork(dim_x, dim_c), mask[i])
                     for i in range(count_couplings)]
        self.couplings: nn.ModuleList = nn.ModuleList(couplings)
        self.couplings_reverse: nn.ModuleList = nn.ModuleList(reversed(couplings))

    def cpu(self):
        for it in self.couplings:
            it.cpu()

        return super().cpu()

    def cuda(self, device=None):
        for it in self.couplings:
            it.cuda()
        return super().cuda(device)

    def forward(self, x: torch.Tensor, c: torch.Tensor):
        z = x
        log_det_jacobian = torch.zeros(x.shape[0]).to(x.device)
        for coupling in self.couplings:
            z, log_det_jacobian_coupling = coupling.forward(z, c)
            log_det_jacobian += log_det_jacobian_coupling

        return z, log_det_jacobian

    @torch.jit.export
    def inverse(self, z: torch.Tensor, c: torch.Tensor):
        x: torch.Tensor = z
        log_det_jacobian = torch.zeros(z.shape[0]).to(z.device)
        for coupling in self.couplings_reverse:
            x, log_det_jacobian_coupling = coupling.inverse(x, c)
            log_det_jacobian += log_det_jacobian_coupling

        return x, log_det_jacobian

    @torch.jit.export
    def log_probability(self, x: torch.Tensor, c: torch.Tensor):
        z, log_det_jacobian = self.forward(x, c)

        # LogP(x) ~ N(0, 1)
        logp = self._log_probability_normal(z).sum(dim=1) + log_det_jacobian
        return x, z, logp

    # noinspection PyMethodMayBeStatic
    def _log_probability_normal(self, z):
        # log p(x) -- x ~ N(0, 1)
        return -z.pow(2) / 2 - 0.9189385332046727

    @torch.jit.export
    def sample_z(self, z: torch.Tensor, c: torch.Tensor = torch.empty(0)):
        x, log_det_jacobian = self.inverse(z, c)
        logp = self._log_probability_normal(z).sum(dim=1) + log_det_jacobian
        return x, z, logp

    @torch.jit.export
    def sample(self, c: torch.Tensor = torch.empty(0)):
        z = torch.normal(0., 1., (c.shape[0], self.dim_x), device=c.device)
        return self.sample_z(z, c)


# TODO rename to kernel, define interfaces, fit, sample, predict, evaluate
@attr.s(slots=True)
class NvpKernel:
    @attr.s(slots=True, frozen=True)
    class Result:
        x: typing.List[np.ma.masked_array] = attr.ib()
        z: typing.List[np.ma.masked_array] = attr.ib()
        logp: np.ma.masked_array = attr.ib()

    id: uuid.UUID = attr.ib(factory=uuid.uuid4)
    pipeline: transformation.Pipeline = attr.ib(factory=transformation.Pipeline.default)
    conditionals: np.ndarray = attr.ib(default=None)
    conditionals_weight: np.ndarray = attr.ib(default=None)
    feature_description_x: "common.FeatureDescription" = attr.ib(init=False)
    feature_description_z: "common.FeatureDescription" = attr.ib(init=False)
    _model: Nvp = attr.ib(init=False)

    def _init_conditionals(self, arr: np.ndarray):
        assert arr is not None
        unique, counts = np.unique(arr, return_counts=True)
        self.conditionals = unique
        self.conditionals_weight = counts / counts.sum()

    def fit(self, arrs: typing.List[np.ndarray], feature_description: "common.FeatureDescription",
            hyper_parameters: "common.HyperParameters") -> "common.TrainingProgress":
        assert arrs is not None and feature_description is not None and hyper_parameters is not None
        assert len(arrs) == len(feature_description.features)
        assert all([it.ndim == 2 for it in arrs])
        assert all([arr.shape[1] == f.extend for arr, f in zip(arrs, feature_description.features)])

        feature_description_x_p = self.pipeline.fit(arrs, feature_description)
        self.feature_description_x = feature_description
        self.feature_description_z = attr.evolve(feature_description,
                                                 features=tuple(feature_description_x_p.features[1:]))

        dim_c = feature_description.feature_conditional().extend
        dim_x = sum([it.extend for it in feature_description.feature_code_elements()])
        mask = _mask_checkboard((hyper_parameters.count_couplings, dim_x), feature_description.feature_indexes())

        arrs_transformed = self.pipeline.transform(arrs)
        self._model = Nvp(hyper_parameters.count_couplings, dim_x, dim_c, mask)
        optimizer = optimization.BatchLikelihoodOptimizer(self.id)

        with util.Timer() as t:
            progress = optimizer.fit(self._model, arrs_transformed, hyper_parameters)
            _logger.debug(f"Fitting finished {feature_description.element_id} {t}")

        self._init_conditionals(arrs[0])
        return progress

    def sample(self, sample_count: int, conditional: np.ndarray = None) -> "NvpKernel.Result":
        assert sample_count and sample_count > 0

        if conditional is None:
            conditional = np.random.choice(self.conditionals, sample_count, p=self.conditionals_weight)[:, np.newaxis]

        arr_c = self.pipeline.transform_feature(conditional, self.feature_description_x.feature_conditional())
        with torch.no_grad():
            x, z, logp = self._model.sample(torch.from_numpy(arr_c))

            arr_x_p = np.split(x.cpu().numpy(), self.pipeline.feature_description_output.feature_splits_code_elements(),
                               axis=1)
            arr_x = self.pipeline.inverse([arr_c] + arr_x_p)

            arr_z = np.split(z.cpu().numpy(), self.feature_description_z.feature_splits_code_elements(), axis=1)

        return NvpKernel.Result(
            arr_x,
            [np.ma.masked_invalid(it) for it in arr_z],
            np.ma.masked_invalid(logp.cpu().numpy())
        )


class Kernel:
    def fit(self, arrs: typing.List[np.ndarray], feature_description: "common.FeatureDescription",
            hyper_parameters: "common.HyperParameters") -> "common.TrainingProgress":
        pass

    def sample(self, sample_count: int, conditional: np.ndarray = None) -> "NvpKernel.Result":
        pass

    def predict(self):
        pass

    def log_likelihood(self):
        pass


class LazyKernel:
    id_kernel: uuid.UUID = attr.ib()
    kernel: NvpKernel = attr.ib()

    def fit(self, arrs: typing.List[np.ndarray], feature_description: "common.FeatureDescription",
            hyper_parameters: "common.HyperParameters") -> "common.TrainingProgress":
        pass

    def sample(self, sample_count: int, conditional: np.ndarray = None) -> "NvpKernel.Result":
        pass

    def predict(self):
        pass

    def log_likelihood(self):
        pass
