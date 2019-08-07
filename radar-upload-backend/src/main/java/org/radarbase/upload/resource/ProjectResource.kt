package org.radarbase.upload.resource

import org.radarbase.auth.jersey.Auth
import org.radarbase.auth.jersey.Authenticated
import org.radarbase.auth.jersey.NeedsPermission
import org.radarbase.upload.dto.ProjectList
import org.radarbase.upload.dto.UserList
import org.radarbase.upload.service.UploadProjectService
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
        @Context private val projectService: UploadProjectService,
        @Context private val auth: Auth) {

    @GET
    @NeedsPermission(Permission.Entity.PROJECT, Permission.Operation.READ)
    fun projects() = ProjectList(projectService.userProjects(auth))

    @GET
    @Path("{projectId}/users")
    @NeedsPermission(Permission.Entity.PROJECT, Permission.Operation.READ, "projectId")
    fun users(@PathParam("projectId") projectId: String): UserList {
        return UserList(projectService.projectUsers(projectId))
    }
}
