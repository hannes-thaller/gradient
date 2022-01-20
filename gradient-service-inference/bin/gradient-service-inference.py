import logging
import signal

from gradient_inference import container

_ONE_DAY_IN_SECONDS = 60 * 60 * 24
_MB = 1024 ** 2

logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
                    level=logging.DEBUG)
logging.getLogger("gradient_inference").setLevel(logging.DEBUG)
logger = logging.getLogger(__name__)

location = "in container" if container.SystemContainer.INSTANCE.in_container() else "locally"
logger.info(f"Model service running {location}")
logger.info(f"Pulsar at {container.PersistenceContainer.INSTANCE.url_pulsar()}")
logger.info(f"MongoDB at {container.PersistenceContainer.INSTANCE.url_mongo()}")

if __name__ == '__main__':
    service = container.ServiceContainer.INSTANCE.service_inference()
    signal.sigwait([signal.SIGINT])
