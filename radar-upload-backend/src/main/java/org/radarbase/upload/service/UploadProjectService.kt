package org.radarbase.upload.service

import org.radarbase.auth.jersey.Auth
import org.radarbase.auth.jersey.ProjectService
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.User

interface UploadProjectService : ProjectService {
    fun userProjects(auth: Auth): List<Project>
    fun projectUsers(projectId: String): List<User>
}
