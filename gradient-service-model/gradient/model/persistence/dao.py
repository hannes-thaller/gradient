import logging
import typing

import attr
import gridfs

if typing.TYPE_CHECKING:
    import uuid
    import pymongo
    from . import serde
    from ..entity import common

_logger = logging.getLogger(__name__)


@attr.s(frozen=True, slots=True)
class ModelDao:
    _mongo_client: "pymongo.MongoClient" = attr.ib()
    _col_feature_descriptions: "pymongo.collection.Collection" = attr.ib()
    _col_model_handles: "pymongo.collection.Collection" = attr.ib()
    _col_kernels: "gridfs.GridFS" = attr.ib()
    _serde: "serde.MongoSerde" = attr.ib()

    @classmethod
    def new(cls, mongo_client: "pymongo.MongoClient"):
        db = mongo_client.get_database("service")

        col_feature_descriptions = db.create_collection("featureDescriptions")
        col_model_handles = db.create_collection("modelHandles")
        col_kernels = gridfs.GridFS(db, "kernels")
        return cls(mongo_client, col_feature_descriptions, col_model_handles, col_kernels)

    def store_feature_descriptions(
            self,
            feature_descriptions: typing.Iterable["common.FeatureDescription"]
    ) -> typing.List[uuid.UUID]:
        assert feature_descriptions

        docs = [self._serde.to_feature_description(it) for it in feature_descriptions]

        ids = []
        if docs:
            result = self._col_feature_descriptions.insert_many(docs)
            ids.append(result.inserted_ids)

        return ids

    def load_feature_descriptions(self):
        pass


