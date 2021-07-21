package org.sourceflow.gradient.project.persistence

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sourceflow.gradient.project.entity.CanonicalName
import org.sourceflow.gradient.project.DIContainer
import org.sourceflow.gradient.project.entity.NameComponentType
import org.sourceflow.gradient.project.entity.Project
import java.util.*

class ProjectDaoIntegrationTest : StringSpec({
    val projectDao = DIContainer.projectDao

    "should save and load project"{
        val name = CanonicalName.from(
                "org" to NameComponentType.GROUP,
                "sourceflow" to NameComponentType.GROUP,
                "gradient" to NameComponentType.ARTIFACT,
                "0.1.0" to NameComponentType.VERSION
        )
        val project = Project(name, UUID.randomUUID(), listOf(UUID.randomUUID()))

        projectDao.saveProject(project) shouldBe project
        projectDao.loadProjectByName(name) shouldBe project
        projectDao.loadProjectBySession(project.sessions.first()) shouldBe project
    }

    "should update sessions"{
        val name = CanonicalName.from(
                "org" to NameComponentType.GROUP,
                "sourceflow" to NameComponentType.GROUP,
                "gradient" to NameComponentType.ARTIFACT,
                "0.1.1" to NameComponentType.VERSION
        )
        val project = Project(name, UUID.randomUUID(), listOf(UUID.randomUUID()))

        val newId = listOf(UUID.randomUUID(), project.sessions.first())
        projectDao.saveProject(project) shouldBe project
        projectDao.setSessions(project.projectId, newId)
        projectDao.loadProjectByName(name)?.sessions shouldBe newId
    }
})