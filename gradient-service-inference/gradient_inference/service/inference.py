import attr
import logging
import pulsar
from pulsar import schema

from gradient_domain.entities import code as code_pb
from .. import persistence, transformation

_logger = logging.getLogger(__name__)


@attr.s(frozen=True)
class InferenceService:
    _client: "pulsar.Client" = attr.ib()
    _serde: "persistence.ProtobufSerde" = attr.ib()
    _dao: "persistence.ModelDao" = attr.ib()
    _consumer_structure_graph: "pulsar.Consumer" = attr.ib(init=False)
    _consumer_dataset: "pulsar.Consumer" = attr.ib(init=False)
    _producer_inference_graph: "pulsar.Producer" = attr.ib(init=False)

    def __attrs_post_init__(self):
        consumer_dataset = self._client.subscribe(
            topic="gradient.feature.dataset.created.v1",
            subscription_name="gradient.service.inference.v1",
            message_listener=self._listen_dataset
        )
        consumer_structure_graph = self._client.subscribe(
            topic="gradient.code.structure-graph.created.v1",
            subscription_name="gradient.service.inference.v1",
            message_listener=self._listen_structure_graph,
            schema=schema.BytesSchema()
        )
        producer_inference_graph = self._client.create_producer(
            topic="gradient.inference.inference-graph.created.v1",
            producer_name="gradient.service.inference.v1",
        )

        object.__setattr__(self, "_consumer_structure_graph", consumer_structure_graph)
        object.__setattr__(self, "_consumer_dataset", consumer_dataset)
        object.__setattr__(self, "_producer_inference_graph", producer_inference_graph)

    def _listen_structure_graph(self, consumer, message):
        _logger.info(f"Receiving structure graph")

        msg = code_pb.CodeMessage()
        msg.ParseFromString(message.value())

        project_context = self._serde.from_project_context(message.project_context)
        graph_structure = self._serde.from_structure_graph(msg.structure_graph)
        graph_factor = transformation.FactorGraphTransformation.transform(graph_structure)

        self._dao.store_factor_graph(project_context, graph_factor)
        consumer.acknowledge(message)

    def _listen_dataset(self, consumer, message):
        _logger.info(f"Receiving dataset message")

        consumer.acknowledge(message)

    def close(self):
        self._consumer_structure_graph.unsubscribe()
        self._consumer_dataset.unsubscribe()
