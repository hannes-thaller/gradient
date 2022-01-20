import pytest

from gradient_domain.entities import common
from gradient_inference import container


@pytest.mark.skip
def test_jvm_python():
    client = container.PersistenceContainer().client_pulsar()

    consumer = client.subscribe("jvm-python", "tester python")

    producer = client.create_producer("python-jvm", "tester python")

    msg_consumer = consumer.receive()

    msg = common.CanonicalName()
    msg.ParseFromString(msg_consumer.value())

    producer.send(msg.SerializeToString())

    consumer.unsubscribe()
    client.close()
