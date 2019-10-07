/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.upload.service.managementportal

import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.upload.util.CachedSet
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.User
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
            throw HttpNotFoundException("project_not_found", "Project $projectId not found.")
        }
    }

    override fun userProjects(auth: Auth): List<Project> {
        return projects.get()
                .filter { auth.token.hasPermissionOnProject(Permission.PROJECT_READ, it.id) }
    }

    override fun project(projectId: String) : Project = projects.find { it.id == projectId } ?:
        throw HttpNotFoundException("project_not_found", "Project $projectId not found.")

    override fun projectUsers(projectId: String): List<User> {
        val projectParticipants = participants.computeIfAbsent(projectId) {
            CachedSet(Duration.ofMinutes(30), Duration.ofMinutes(1)) {
                mpClient.readParticipants(projectId)
            }
        }

        return projectParticipants.get().toList()
    }
}
