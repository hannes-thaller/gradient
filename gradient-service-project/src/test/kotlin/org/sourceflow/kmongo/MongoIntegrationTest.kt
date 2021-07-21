package org.sourceflow.kmongo

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters.eq
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.sourceflow.gradient.project.DIContainer
import java.util.*

class MongoIntegrationTest : StringSpec({
    val client = DIContainer.mongoClient
    val database = client.getDatabase("test")
    val col = database.getCollection("test")

    afterSpec {
        withContext(Dispatchers.IO) {
            client.close()
        }
    }

    "should store and load jedi"{
        val kylo = Document(mapOf(
                "name" to "Kylo",
                "force" to "LIGHT",
                "id" to UUID.randomUUID()
        ))
        val windou = Document(mapOf(
                "name" to "Windou",
                "force" to "DARK",
                "id" to UUID.randomUUID()
        ))

        col.insertMany(listOf(kylo, windou))

        col.find(eq("name", "Kylo")).toList() shouldContain kylo
        col.find(eq("force", "DARK")).toList() shouldContain windou
    }
})