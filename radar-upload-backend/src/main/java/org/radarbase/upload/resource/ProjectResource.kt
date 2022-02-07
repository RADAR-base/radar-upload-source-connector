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

package org.radarbase.upload.resource

import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.cache.Cache
import org.radarbase.jersey.service.managementportal.RadarProjectService
import org.radarbase.upload.dto.*
import org.radarbase.auth.authorization.Permission
import jakarta.annotation.Resource
import jakarta.inject.Singleton
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Path("projects")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Singleton
class ProjectResource(
        @Context private val projectService: RadarProjectService,
        @Context private val auth: Auth
) {

    @GET
    @NeedsPermission(Permission.Entity.PROJECT, Permission.Operation.READ)
    @Cache(maxAge = 300, isPrivate = true)
    fun projects() = ProjectList(projectService.userProjects(auth)
            .map { it.toProject() })

    @GET
    @Path("{projectId}/users")
    @Cache(maxAge = 300, isPrivate = true)
    @NeedsPermission(Permission.Entity.PROJECT, Permission.Operation.READ, "projectId")
    fun users(@PathParam("projectId") projectId: String): UserList {
        return UserList(projectService.projectUsers(projectId)
                .map { it.toUser() })
    }

    @GET
    @Path("{projectId}")
    @Cache(maxAge = 3600, isPrivate = true)
    @NeedsPermission(Permission.Entity.PROJECT, Permission.Operation.READ, "projectId")
    fun project(@PathParam("projectId") projectId: String): Project {
        return projectService.project(projectId).toProject()
    }
}
