package org.radarbase.upload.inject

import org.radarbase.jersey.auth.ProjectService
import org.radarbase.upload.service.UploadProjectService
import javax.inject.Provider
import javax.ws.rs.core.Context

class ProjectServiceWrapper(
        @Context private val mpProjectService: Provider<UploadProjectService>
): ProjectService {
    override fun ensureProject(projectId: String) = mpProjectService.get().ensureProject(projectId)
}
