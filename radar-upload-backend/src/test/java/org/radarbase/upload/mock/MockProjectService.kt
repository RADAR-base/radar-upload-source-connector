package org.radarbase.upload.mock

import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.management.client.MPProject
import org.radarbase.management.client.MPSubject
import org.radarbase.jersey.service.managementportal.RadarProjectService
import org.radarbase.auth.authorization.Permission

class MockProjectService(private val projects: Map<String, List<String>>) : RadarProjectService {
    override fun userByExternalId(projectId: String, externalUserId: String): MPSubject? {
        return MPSubject(projectId = projectId, id = "something", externalId = externalUserId, status = "ACTIVATED")
    }

    override fun project(projectId: String): MPProject {
        ensureProject(projectId)
        return MPProject(id = projectId)
    }

    override fun userProjects(auth: Auth, permission: Permission): List<MPProject> = projects.keys.map { MPProject(id = it) }

    override fun projectUsers(projectId: String): List<MPSubject> {
        ensureProject(projectId)
        return projects.getValue(projectId)
                .map { MPSubject(projectId = projectId, id = it, status = "ACTIVATED") }
    }

    override fun ensureProject(projectId: String) {
        if (projectId !in projects) {
            throw HttpNotFoundException("project_not_found", "Project $projectId does not exist")
        }
    }

    override fun ensureUser(projectId: String, userId: String) {
        ensureProject(projectId)

        if (userId !in projects.getValue(projectId)) {
            throw HttpNotFoundException("user_not_found", "User $userId does not exist in project $projectId")
        }
    }

    override fun getUser(projectId: String, userId: String): MPSubject? {
        ensureUser(projectId, userId)
        return MPSubject(projectId = projectId, id = userId, status = "ACTIVATED")
    }
}
