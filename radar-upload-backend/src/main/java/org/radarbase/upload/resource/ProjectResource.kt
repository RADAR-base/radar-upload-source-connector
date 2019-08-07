package org.radarbase.upload.resource

import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.auth.NeedsPermissionOnProject
import org.radarbase.upload.dto.ProjectList
import org.radarbase.upload.dto.User
import org.radarbase.upload.dto.UserList
import org.radarbase.upload.service.MPService
import org.radarcns.auth.authorization.Permission
import javax.annotation.Resource
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("/projects")
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
}
