package org.radarbase.upload.mock

import org.radarbase.auth.authorization.Permission
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.service.managementportal.RadarProjectService
import org.radarbase.management.client.MPOrganization
import org.radarbase.management.client.MPProject
import org.radarbase.management.client.MPSubject

class MockProjectService(private val projects: Map<String, List<String>>) : RadarProjectService {
    private val org = MPOrganization("main")

    override suspend fun subjectByExternalId(projectId: String, externalUserId: String): MPSubject? {
        return MPSubject(projectId = projectId, id = "something", externalId = externalUserId, status = "ACTIVATED")
    }

    override suspend fun project(projectId: String): MPProject {
        ensureProject(projectId)
        return MPProject(id = projectId, organization = org)
    }

    override suspend fun projectOrganization(projectId: String): String {
        ensureProject(projectId)
        return org.id
    }

    override suspend fun userProjects(permission: Permission): List<MPProject> {
        return projects.keys.map { MPProject(id = it, organization = org) }
    }

    override suspend fun projectSubjects(projectId: String): List<MPSubject> {
        ensureProject(projectId)
        return projects.getValue(projectId)
            .map { MPSubject(projectId = projectId, id = it, status = "ACTIVATED") }
    }

    override suspend fun ensureOrganization(organizationId: String) {
        if (organizationId != org.id) {
            throw HttpNotFoundException("user_not_found", "Organization $organizationId does not exist")
        }
    }

    override suspend fun ensureProject(projectId: String) {
        if (projectId !in projects) {
            throw HttpNotFoundException("project_not_found", "Project $projectId does not exist")
        }
    }

    override suspend fun ensureSubject(projectId: String, userId: String) {
        ensureProject(projectId)

        if (userId !in projects.getValue(projectId)) {
            throw HttpNotFoundException("user_not_found", "User $userId does not exist in project $projectId")
        }
    }

    override suspend fun listProjects(organizationId: String): List<String> {
        ensureOrganization(organizationId)
        return projects.keys.toList()
    }

    override suspend fun subject(projectId: String, userId: String): MPSubject {
        ensureSubject(projectId, userId)
        return MPSubject(projectId = projectId, id = userId, status = "ACTIVATED")
    }
}
