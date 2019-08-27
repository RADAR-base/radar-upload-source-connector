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

package org.radarbase.upload.service

import org.radarbase.upload.util.CachedSet
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
        if (projects.find { it.id == name } == null) {
            throw NotFoundException("project_not_found", "Project $name not found.")
        }
    }

    fun userProjects(auth: Auth): List<Project> {
        return projects.get()
                .filter { auth.hasPermissionOnProject(Permission.PROJECT_READ, it.id) }
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
