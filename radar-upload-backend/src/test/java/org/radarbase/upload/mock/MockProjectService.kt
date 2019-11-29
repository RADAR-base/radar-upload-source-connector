package org.radarbase.upload.mock

import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.User
import org.radarbase.upload.service.UploadProjectService

class MockProjectService(private val projects: Map<String, List<String>>) : UploadProjectService {

    override fun userByExternalId(projectId: String, externalUserId: String): User? {
        return User(projectId = projectId, id = "something", externalId = externalUserId, status = "ACTIVATED")
    }

    override fun project(projectId: String): Project {
        ensureProject(projectId)
        return Project(id = projectId)
    }

    override fun userProjects(auth: Auth) = projects.keys.map { Project(id = it) }

    override fun projectUsers(projectId: String): List<User> {
        ensureProject(projectId)
        return projects.getValue(projectId)
                .map { User(projectId = projectId, id = it, status = "ACTIVATED") }
    }

    override fun ensureProject(projectId: String) {
        if (projectId !in projects) {
            throw HttpNotFoundException("project_not_found", "Project $projectId does not exist")
        }
    }

}
