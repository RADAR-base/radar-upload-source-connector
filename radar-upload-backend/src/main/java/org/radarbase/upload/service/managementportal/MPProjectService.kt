package org.radarbase.upload.service.managementportal

import org.radarbase.appconfig.util.CachedSet
import org.radarbase.auth.jersey.Auth
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.User
import org.radarbase.upload.exception.NotFoundException
import org.radarbase.upload.service.UploadProjectService
import org.radarcns.auth.authorization.Permission
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.ws.rs.core.Context

class MPProjectService(@Context private val mpClient: MPClient): UploadProjectService {
    private val projects = CachedSet(
            Duration.ofMinutes(30),
            Duration.ofMinutes(1)) {
        mpClient.readProjects()
    }

    private val participants: ConcurrentMap<String, CachedSet<User>> = ConcurrentHashMap()

    override fun ensureProject(projectId: String) {
        if (projects.find { it.id == projectId } == null) {
            throw NotFoundException("project_not_found", "Project $projectId not found.")
        }
    }

    override fun userProjects(auth: Auth): List<Project> {
        return projects.get()
                .filter { auth.hasPermissionOnProject(Permission.PROJECT_READ, it.id) }
    }

    override fun projectUsers(projectId: String): List<User> {
        val projectParticipants = participants.computeIfAbsent(projectId) {
            CachedSet(Duration.ofMinutes(30), Duration.ofMinutes(1)) {
                mpClient.readParticipants(projectId)
            }
        }

        return projectParticipants.get().toList()
    }
}
