import logging
import typing

import attr

from src.main.python.gradient.model.api import model_api_pb2_grpc
from src.main.python.gradient.model.api import dataset_entity_pb2

if typing.TYPE_CHECKING:
    import pulsar

_logger = logging.getLogger(__name__)


@attr.s(frozen=True, slots=True)
class MessageService(model_api_pb2_grpc.ModelServiceServicer):
    _client: "pulsar.Client" = attr.ib()
    _consumer_dataset: "pulsar.Consumer" = attr.ib(init=False)
    _consumer_model: "pulsar.Consumer" = attr.ib(init=False)
    _producer_model: "pulsar.Producer" = attr.ib(init=False)
    _dataset_actions: typing.Dict = attr.ib(init=False)

    def __attrs_post_init__(self):
        dataset_machine = {
            "dataset_handle_detail": self.handle_dataset_handle_detail
        }

        consumer_code = self._client.subscribe("code", "gs-model-service",
                                               message_listener=self.listen_for_code_messages)
        consumer_dataset = self._client.subscribe("dataset", "gs-model-service",
                                                  message_listener=self.listen_for_dataset_messages)
        consumer_model = self._client.subscribe("model", "gs-model-service",
                                                message_listener=self.listen_for_model_messages)

        object.__setattr__(self, "_dataset_actions", dataset_machine)
        object.__setattr__(self, "_consumer_code", consumer_code)
        object.__setattr__(self, "_consumer_dataset", consumer_dataset)
        object.__setattr__(self, "_consumer_model", consumer_model)
        object.__setattr__(self, "_producer_model", self._client.create_producer("model"))

    def listen_for_dataset_messages(self, consumer: "pulsar.Consumer", msg: "pulsar.Message"):
        msg_dataset = dataset_entity_pb2.DatasetMessage()
        msg_dataset.ParseFromString(msg.data())

        fn_action = self._dataset_actions.get(msg_dataset.WhichOneof("payload"), None)
        if fn_action:
            fn_action(msg_dataset)

        consumer.acknowledge(msg)

    def listen_for_model_messages(self, consumer: "pulsar.Consumer", msg: "pulsar.Message"):
        _logger.debug(f"Acknowledging irrelevant model message.")

        consumer.acknowledge(msg)

    def listen_for_code_messages(self, consumer: "pulsar.Consumer", msg: "pulsar.Message"):
        _logger.debug(f"Acknowledging new code message")

    def handle_dataset_handle_detail(self, msg: "dataset_entity_pb2.DatasetMessage"):
        assert (msg.HasField("dataset_handle_detail"))
        # TODO issue model training
        # TODO add dataset handle to queue, retrive dataset, fit model, store model, emit model handle, update code elements with "hasModel"
        _logger.debug(f"Received new dataset handle.")

    def close(self):
        self._consumer_model.close()
        self._consumer_dataset.close()
        self._producer_model.close()
