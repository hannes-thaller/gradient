import typing
import uuid

import attr


@attr.s(slots=True, frozen=True)
class HyperParameters:
    learning_rate: float = attr.ib(default=5e-4)
    batch_size: int = attr.ib(default=128)
    weight_decay: float = attr.ib(default=5e-2)
    max_epoch: int = attr.ib(default=500)
    cuda: bool = attr.ib(default=False)
    count_couplings: int = attr.ib(default=6)


@attr.s(slots=True)
class TrainingProgress:
    loss_record: typing.List[dict] = attr.ib(factory=list)

    def report_epoch(self, kernel_id: uuid.UUID, epoch: int, loss: float, tags: typing.List[str]):
        self.loss_record.append({
            "kernel_id": str(kernel_id),
            "epoch": epoch,
            "loss": loss,
            "tags": tags
        })
