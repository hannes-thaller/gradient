import concurrent.futures
import logging
import signal

import grpc
from . import container
from gradient_domain.services import inference

_ONE_DAY_IN_SECONDS = 60 * 60 * 24
_MB = 1024 ** 2

_logger = logging.getLogger(__name__)


def main():
    location = "in container" if container.SystemContainer.INSTANCE.in_container() else "locally"
    _logger.info(f"Model service running {location}")
    _logger.info(f"GRPC at {container.PersistenceContainer.INSTANCE.grpc_port()}")
    _logger.info(f"Pulsar at {container.PersistenceContainer.INSTANCE.url_pulsar()}")
    _logger.info(f"MongoDB at {container.PersistenceContainer.INSTANCE.url_mongo()}")

    # TODO move into container
    server_options = [
        ('grpc.max_send_message_length', 64 * _MB),
        ('grpc.max_receive_message_length', 64 * _MB)
    ]
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
        server = grpc.server(pool, options=server_options)
        server.add_insecure_port("[::]:50001")
        service = container.ServiceContainer.INSTANCE.service_model()
        inference.add_ModelServiceServicer_to_server(service, server)

        server.start()
        signal.sigwait([signal.SIGINT])
        server.stop(0)
