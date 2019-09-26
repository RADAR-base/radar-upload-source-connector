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

package org.radarbase.upload.filter

import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.auth.NeedsPermissionOnProject
import org.radarbase.upload.auth.NeedsPermissionOnUser
import org.radarbase.upload.service.MPService
import org.radarcns.auth.authorization.Permission
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * Check that the token has given permissions.
 */
class PermissionFilter : ContainerRequestFilter {

    @Context
    private lateinit var resourceInfo: ResourceInfo

    @Context
    private lateinit var auth: Auth

    @Context
    private lateinit var mpService: MPService

    @Context
    private lateinit var uriInfo: UriInfo

    override fun filter(requestContext: ContainerRequestContext) {
        val resourceMethod = resourceInfo.resourceMethod

        val userAnnotation = resourceMethod.getAnnotation(NeedsPermissionOnUser::class.java)
        val projectAnnotation = resourceMethod.getAnnotation(NeedsPermissionOnProject::class.java)
        val annotation = resourceMethod.getAnnotation(NeedsPermission::class.java)

        val (permission, project, isAuthenticated) = when {
            userAnnotation != null -> {
                val permission = Permission(userAnnotation.entity, userAnnotation.operation)
                val projectId = uriInfo.pathParameters[userAnnotation.projectPathParam]?.firstOrNull()
                val userId = uriInfo.pathParameters[userAnnotation.userPathParam]?.firstOrNull()

                Triple(permission, projectId, projectId != null
                        && userId != null
                        && auth.hasPermissionOnSubject(permission, projectId, userId))
            }
            projectAnnotation != null -> {
                val permission = Permission(projectAnnotation.entity, projectAnnotation.operation)

                val projectId = uriInfo.pathParameters[projectAnnotation.projectPathParam]?.firstOrNull()

                Triple(permission, projectId, projectId != null
                        && auth.hasPermissionOnProject(permission, projectId))
            }
            annotation != null -> {
                val permission = Permission(annotation.entity, annotation.operation)

                Triple(permission, null, auth.hasPermission(permission))
            }
            else -> return
        }

        if (!isAuthenticated) {
            abortWithForbidden(requestContext, permission)
            return
        }
        project?.let { mpService.ensureProject(it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionFilter::class.java)

        /**
         * Abort the request with a forbidden status. The caller must ensure that no other changes are
         * made to the context (i.e., make a quick return).
         * @param requestContext context to abort
         * @param scope the permission that is needed.
         */
        fun abortWithForbidden(requestContext: ContainerRequestContext, scope: Permission) {
            val message = "$scope permission not given."
            logger.warn("[403] {}: {}",
                    requestContext.uriInfo.path, message)

            requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                            .header("WWW-Authenticate", AuthenticationFilter.BEARER_REALM
                                    + " error=\"insufficient_scope\""
                                    + " error_description=\"$message\""
                                    + " scope=\"$scope\"")
                            .build())
        }
    }
}
