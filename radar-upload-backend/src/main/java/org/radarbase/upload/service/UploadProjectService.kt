package org.radarbase.upload.service

import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.ProjectService
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.User

interface UploadProjectService : ProjectService {
    fun project(projectId: String): Project
    fun userProjects(auth: Auth): List<Project>
    fun projectUsers(projectId: String): List<User>
    fun userByExternalId(projectId: String, externalUserId: String): User?
}
