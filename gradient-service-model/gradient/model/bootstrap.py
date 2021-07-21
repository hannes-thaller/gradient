import concurrent.futures
import logging
import signal

import grpc

from gradient.model import ServiceContainer, SystemContainer, PersistenceContainer
from gradient.model.api import model_api_pb2_grpc

_ONE_DAY_IN_SECONDS = 60 * 60 * 24
_MB = 1024 ** 2

_logger = logging.getLogger(__name__)


def main():
    location = "in container" if SystemContainer.INSTANCE.in_container() else "locally"
    _logger.info(f"Model service running {location}")
    _logger.info(f"GRPC at {PersistenceContainer.INSTANCE.grpc_port()}")
    _logger.info(f"Pulsar at {PersistenceContainer.INSTANCE.url_pulsar()}")
    _logger.info(f"MongoDB at {PersistenceContainer.INSTANCE.url_mongo()}")

    # TODO move into container
    server_options = [
        ('grpc.max_send_message_length', 64 * _MB),
        ('grpc.max_receive_message_length', 64 * _MB)
    ]
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
        server = grpc.server(pool, options=server_options)
        server.add_insecure_port("[::]:50001")
        service = ServiceContainer.INSTANCE.service_model()
        model_api_pb2_grpc.add_ModelServiceServicer_to_server(service, server)

        server.start()
        signal.sigwait([signal.SIGINT])
        server.stop(0)
