import atexit
import operator
import os
import typing

import attr

if typing.TYPE_CHECKING:
    import pulsar
    import pymongo
    from . import service, persistence
    from .api import code_entity_pb2
    from gradient.model.persistence import dao


@attr.s(frozen=True, slots=True)
class ShutdownCleaner:
    _cleanup_routines: typing.List[typing.Tuple[typing.Callable, int]] = attr.ib(factory=list, init=False)

    def add_cleanup_routine(self, fn_clean: typing.Callable, priority: int):
        assert fn_clean
        self._cleanup_routines.append((fn_clean, priority))

    def cleanup(self):
        routines = sorted(self._cleanup_routines, key=operator.itemgetter(1), reverse=True)
        for it in routines:
            it[0]()


@attr.s(slots=True)
class SystemContainer:
    _shutdown_cleaner: "ShutdownCleaner" = attr.ib(init=False)
    INSTANCE: "SystemContainer" = None

    def __attrs_post_init__(self):
        self._log_configuration()

    @staticmethod
    def in_container() -> bool:
        return "GRADIENT_DOCKER" in os.environ

    def shutdown_cleaner(self):
        if not self._shutdown_cleaner:
            self._shutdown_cleaner = ShutdownCleaner()
            atexit.register(self._shutdown_cleaner.cleanup)
        return self._shutdown_cleaner

    @staticmethod
    def _log_configuration():
        import logging
        import sys
        stream_handler = logging.StreamHandler(sys.stdout)
        logging.basicConfig(level=logging.INFO,
                            handlers=[stream_handler],
                            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        logging.getLogger("gradient.model.service").setLevel(logging.DEBUG)
        logging.getLogger("gradient.model.dao").setLevel(logging.DEBUG)
        logging.getLogger("gradient.model.inference").setLevel(logging.DEBUG)
        logging.getLogger("gradient.model.optimization").setLevel(logging.DEBUG)


@attr.s(slots=True)
class PersistenceContainer:
    _client_pulsar = attr.ib(init=False)
    _client_mongo = attr.ib(init=False)
    _dao_model = attr.ib(init=False)
    INSTANCE: "PersistenceContainer" = None

    @staticmethod
    def grpc_port() -> int:
        return 15001

    @staticmethod
    def url_pulsar() -> str:
        if SystemContainer.INSTANCE.in_container():
            return "pulsar://gs-message-database:6650"
        else:
            return "pulsar://localhost:10002"

    @staticmethod
    def url_mongo() -> str:
        if SystemContainer.INSTANCE.in_container():
            return "mongodb://gs-code-database:27017"
        else:
            return "mongodb://localhost:12002"

    def client_pulsar(self) -> "pulsar.Client":
        if self._client_pulsar is None:
            import pulsar
            self._client_pulsar = pulsar.Client(self.url_pulsar())
            SystemContainer.INSTANCE.shutdown_cleaner().add_cleanup_routine(self._client_pulsar.close, 1)

        return self._client_pulsar

    def client_mongo(self) -> "pymongo.MongoClient":
        if self._client_mongo is None:
            import pymongo
            self._client_mongo = pymongo.MongoClient(self.url_mongo())
            SystemContainer.INSTANCE.shutdown_cleaner().add_cleanup_routine(self._client_mongo.close, 1)

        return self._client_mongo

    def dao_model(self) -> "dao.ModelDao":
        if self._dao_model is None:
            from . import persistence
            self._dao_model = persistence.ModelDao.new(self.client_mongo())
        return self._dao_model

    def protobuf_serde(self) -> "persistence.ProtobufSerde":
        from gradient.model import persistence
        return persistence.ProtobufSerde()

    def factory_program_repository(self) \
            -> typing.Callable[["code_entity_pb2.ProgramDetail"], "persistence.ProgramRepository"]:
        from gradient.model import persistence

        def factory(program):
            assert program
            return persistence.ProgramRepository(program, self.protobuf_serde())

        return factory


@attr.s(slots=True)
class ServiceContainer:
    _service_model: "service.MessageService" = attr.ib(init=False)
    INSTANCE: "ServiceContainer" = None

    @staticmethod
    def serde_protobuf():
        from gradient.model import persistence
        return persistence.ProtobufSerde()

    def service_model(self) -> "service.MessageService":
        if self._service_model is None:
            from gradient.model import service
            self._service_model = service.MessageService(PersistenceContainer.INSTANCE.client_pulsar())
            SystemContainer.INSTANCE.shutdown_cleaner().add_cleanup_routine(self._service_model.close, 2)
        return self._service_model


SystemContainer.INSTANCE = SystemContainer()
PersistenceContainer.INSTANCE = PersistenceContainer()
ServiceContainer.INSTANCE = ServiceContainer()
