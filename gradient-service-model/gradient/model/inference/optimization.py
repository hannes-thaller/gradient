import typing

import attr
import numpy as np
import torch
from torch import optim
from torch.utils import data

from gradient.model.entity import common

if typing.TYPE_CHECKING:
    import uuid
    import inference


@attr.s(slots=True)
class BestParameterTracker:
    best_loss: float = attr.ib(default=float("inf"), init=False)
    best_epoch: int = attr.ib(default=0, init=False)
    _model_state = attr.ib(factory=dict, init=False)

    def track(self, model, epoch, loss: float):
        if self.best_loss > loss:
            self.best_loss = loss
            self.best_epoch = epoch
            self._model_state = {k: (v.detach().cpu() if isinstance(v, torch.Tensor) else v)
                                 for k, v in model.state_dict().items()}

    def set_best_parameters(self, kernel):
        kernel.params.load_state_dict(self._model_state)


@attr.s(slots=True)
class EarlyStopper:
    min_epoch: int = attr.ib()
    patience: int = attr.ib()
    threshold: float = attr.ib()
    signal_stop: callable = attr.ib()
    loss = attr.ib(factory=list, init=False)

    def track(self, epoch, loss: float):
        loss_prev = self.ma_exp(np.array(self.loss[-self.patience:]))[-1]
        if epoch > self.min_epoch and np.abs(loss - loss_prev) < self.threshold:
            self.signal_stop()
        else:
            self.loss.append(loss)

    def ma_exp(self, data):
        if data.shape[0] == 0:
            return [0]

        window = self.patience * 2
        alpha = 2 / (window + 1.0)
        alpha_rev = 1 - alpha
        n = data.shape[0]

        pows = alpha_rev ** (np.arange(n + 1))

        scale_arr = 1 / pows[:-1]
        offset = data[0] * pows[1:]
        pw0 = alpha * alpha_rev ** (n - 1)

        mult = data * pw0 * scale_arr
        cumsums = mult.cumsum()
        out = offset + cumsums * scale_arr[::-1]
        return out


@attr.s(slots=True)
class BatchLikelihoodOptimizer:
    kernel_id: "uuid.UUID" = attr.ib()
    progress: common.TrainingProgress = attr.ib(factory=common.TrainingProgress)
    _early_stopper: "EarlyStopper" = attr.ib(default=None)
    _parameter_tracker: "BestParameterTracker" = attr.ib(factory=BestParameterTracker)
    _training_on: bool = attr.ib(default=False)

    def __attrs_post_init__(self):
        def _stop():
            self._training_on = False

        self._early_stopper = EarlyStopper(50, 20, 0.1, _stop())

    def fit(self, model: "inference.Nvp", arrs: typing.List[np.ma.masked_array], hyper_parameters: common.HyperParameters) -> "common.TrainingProgress":
        assert model is not None and arrs is not None and hyper_parameters is not None
        assert len(arrs) > 1, "The given dataset does only contain conditionals"
        assert all([(~it.mask).any() for it in arrs])

        dataset_training, dataset_test = self._prepare_dataset(arrs)
        self._optimize(model, dataset_training, dataset_test, hyper_parameters)
        return self.progress

    @staticmethod
    def _prepare_dataset(arrs: typing.List[np.ma.masked_array]) -> typing.Tuple[data.TensorDataset, data.TensorDataset]:
        assert len(arrs) > 1
        assert all([it.ndim == 2 for it in arrs])

        mask = ~np.concatenate([it.mask for it in arrs], axis=1).any(1)
        arrs = [it[mask] for it in arrs]

        n = arrs[0].shape[0]

        cutoff = int(n * 0.9)
        indices = np.arange(n)
        np.random.shuffle(indices)
        indices_train = indices[:cutoff]
        indices_test = indices[cutoff:]

        c = arrs[0]
        x = np.concatenate(arrs[1:], axis=1)

        ds_train = data.TensorDataset(
            torch.from_numpy(x[indices_train]),
            torch.from_numpy(c[indices_train])
        )

        ds_test = data.TensorDataset(
            torch.from_numpy(x[indices_test]),
            torch.from_numpy(c[indices_test])
        )

        return ds_train, ds_test

    def _optimize(self, model: "inference.Nvp", dataset_training: data.TensorDataset, dataset_test: data.TensorDataset, hyper_parameters: common.HyperParameters):
        assert model is not None and dataset_training is not None and dataset_test is not None

        optimizer = optim.Adam(model.parameters(), lr=hyper_parameters.learning_rate, weight_decay=hyper_parameters.weight_decay)

        ds_x, ds_c = dataset_training.tensors

        self._training_on = True
        for epoch in range(hyper_parameters.max_epoch):
            if not self._training_on:
                break

            model.train()
            optimizer.zero_grad()

            x, z, logp = model.log_probability(x=ds_x, c=ds_c)
            nll = -logp.mean()

            nll.backward(retain_graph=True)
            optimizer.step()

            loss_training = float(np.mean(nll.detach().cpu().numpy()))
            loss_test = self._evaluate_test(dataset_test, model)

            self.progress.report_epoch(self.kernel_id, epoch, loss_training, ["training"])
            self.progress.report_epoch(self.kernel_id, epoch, loss_test, ["test"])

    @staticmethod
    def _evaluate_test(dataset: data.TensorDataset, model: "inference.Nvp") -> float:
        assert model and dataset is not None

        model.eval()
        x, c = dataset.tensors
        _, __, logp = model.log_probability(x=x, c=c)

        return float(-np.mean(logp.detach().cpu().numpy()))
