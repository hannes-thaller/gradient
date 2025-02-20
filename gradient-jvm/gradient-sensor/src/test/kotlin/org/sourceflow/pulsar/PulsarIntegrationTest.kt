package org.sourceflow.pulsar

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.apache.pulsar.client.api.Schema
import org.sourceflow.gradient.sensor.DIContainer

class PulsarIntegrationTest : StringSpec({
    "should produce message"{
        withContext(Dispatchers.IO) {
            val client = DIContainer.pulsarClient
            val producer = client.newProducer(Schema.STRING)
                .topic("test-connection")
                .create()
            val consumer = client.newConsumer(Schema.STRING)
                .topic("test-connection")
                .subscriptionName("tester")
                .subscribe()

            val result = coroutineScope {
                producer.sendAsync("Hello")
                consumer.receiveAsync().await()
            }

            result.value shouldBe "Hello"

            producer.close()
            consumer.close()
            client.close()
        }
    }
})