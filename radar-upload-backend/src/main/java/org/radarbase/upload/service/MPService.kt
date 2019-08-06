package org.radarbase.upload.service

import org.radarbase.appconfig.util.CachedSet
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.MPClient
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.User
import org.radarbase.upload.exception.NotFoundException
import org.radarcns.auth.authorization.Permission
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.ws.rs.core.Context

class MPService(@Context private val mpClient: MPClient) {
    private val projects = CachedSet(
            Duration.ofMinutes(30),
            Duration.ofMinutes(1)) {
        mpClient.readProjects()
    }

    private val participants: ConcurrentMap<String, CachedSet<User>> = ConcurrentHashMap()

    fun ensureProject(name: String) {
        if (projects.find { it.name == name } == null) {
            throw NotFoundException("project_not_found", "Project $name not found.")
        }
    }

    fun userProjects(auth: Auth): List<Project> {
        return projects.get()
                .filter { auth.hasPermissionOnProject(Permission.PROJECT_READ, it.name) }
    }

    fun projectUsers(projectId: String): List<User> {
        val projectParticipants = participants.computeIfAbsent(projectId) {
            CachedSet(Duration.ofMinutes(30), Duration.ofMinutes(1)) {
                mpClient.readParticipants(projectId)
            }
        }

        return projectParticipants.get().toList()
    }
}
