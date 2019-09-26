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

import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.auth.NeedsPermissionOnProject
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.ProjectList
import org.radarbase.upload.dto.UserList
import org.radarbase.upload.service.MPService
import org.radarcns.auth.authorization.Permission
import javax.annotation.Resource
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("projects")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
class ProjectResource(
        @Context private val mpService: MPService,
        @Context private val auth: Auth) {

    @GET
    @NeedsPermission(Permission.Entity.PROJECT, Permission.Operation.READ)
    fun projects() = ProjectList(mpService.userProjects(auth))

    @GET
    @Path("{projectId}/users")
    @NeedsPermissionOnProject(Permission.Entity.PROJECT, Permission.Operation.READ, "projectId")
    fun users(@PathParam("projectId") projectId: String): UserList {
        return UserList(mpService.projectUsers(projectId))
    }

    @GET
    @Path("{projectId}")
    @NeedsPermissionOnProject(Permission.Entity.PROJECT, Permission.Operation.READ, "projectId")
    fun project(@PathParam("projectId") projectId: String): Project {
        return mpService.project(projectId)
    }
}
