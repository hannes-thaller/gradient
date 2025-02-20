import attr
import logging
import typing

if typing.TYPE_CHECKING:
    import pymongo
    from . import serde
    from .. import entity

_logger = logging.getLogger(__name__)


@attr.s(frozen=True, slots=True)
class ModelDao:
    _mongo_client: "pymongo.MongoClient" = attr.ib()
    _col_factor_graph: "pymongo.collection.Collection" = attr.ib()
    _col_cluster_graph: "pymongo.collection.Collection" = attr.ib()
    _serde: "serde.MongoSerde" = attr.ib()

    @classmethod
    def new(cls, mongo_client: "pymongo.MongoClient", serde_mongo: "serde.MongoSerde"):
        assert mongo_client

        db = mongo_client.get_database("service")

        col_feature_descriptions = db.create_collection("structure-graph")
        return cls(mongo_client, col_feature_descriptions, serde_mongo)

    def store_factor_graph(self, project_context: "entity.ProjectContext", graph_factor: "entity.FactorGraph"):
        assert project_context
        assert graph_factor

        doc = {
            "project": self._serde.to_project_context(project_context),
            "graph_factor": self._serde.to_factor_graph(graph_factor)
        }

        self._col_factor_graph.insert_one(doc)

    def load_factor_graph(self, project_context: "entity.ProjectContext") -> typing.Optional["entity.FactorGraph"]:
        assert project_context

        query = {
            "project": self._serde.to_project_context(project_context)
        }

        return self._col_factor_graph.find_one(query)

    def store_cluster_graph(self, project_context: "entity.ProjectContext", graph_cluster: "entity.ClusterGraph"):
        assert project_context
        assert graph_cluster

        doc = {
            "project": self._serde.to_project_context(project_context),
            "graph_factor": self._serde.to_cluster_graph(graph_cluster)
        }

        self._col_cluster_graph.insert_one(doc)
