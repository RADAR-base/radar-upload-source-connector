package org.radarbase.upload.mock

import org.glassfish.jersey.internal.inject.AbstractBinder
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.AuthConfig
import org.radarbase.jersey.auth.AuthValidator
import org.radarbase.jersey.auth.ProjectService
import org.radarbase.jersey.config.ConfigLoader
import org.radarbase.jersey.config.EnhancerFactory
import org.radarbase.jersey.config.JerseyResourceEnhancer
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.exception.HttpUnauthorizedException
import org.radarbase.upload.Config
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.User
import org.radarbase.upload.inject.ProjectServiceWrapper
import org.radarbase.upload.inject.UploadResourceEnhancer
import org.radarbase.upload.service.UploadProjectService
import java.util.*
import javax.inject.Singleton
import javax.ws.rs.container.ContainerRequestContext

class MockResourceEnhancerFactory(
        private val config: Config,
        private val authQueue: Queue<Pair<Auth?, Boolean>>,
        private val projects: Map<String, List<String>>
): EnhancerFactory {
    override fun createEnhancers(): List<JerseyResourceEnhancer> = listOf(
            UploadResourceEnhancer(config),
            ConfigLoader.Enhancers.radar(AuthConfig(jwtResourceName = config.jwtResourceName)),
            object : JerseyResourceEnhancer {
                override fun AbstractBinder.enhance() {
                    bind(ProjectServiceWrapper::class.java)
                            .to(ProjectService::class.java)
                            .`in`(Singleton::class.java)

                    bind(MockProjectService::class.java)
                            .to(UploadProjectService::class.java)
                            .`in`(Singleton::class.java)

                    bind(MockAuthValidator())
                            .to(AuthValidator::class.java)
                }
            },
            ConfigLoader.Enhancers.httpException,
            ConfigLoader.Enhancers.generalException)

    private inner class MockAuthValidator : AuthValidator {
        override fun verify(token: String, request: ContainerRequestContext): Auth? {
            val element: Pair<Auth?, Boolean>? = authQueue.poll()
            MatcherAssert.assertThat("A next authentication attempt is expected", element, Matchers.not(Matchers.nullValue()))
            val (auth, isAuthorized) = requireNotNull(element)
            if (!isAuthorized) {
                throw HttpUnauthorizedException("token_missing", "Not authorized", listOf())
            }
            return auth
        }
    }

    private inner class MockProjectService : UploadProjectService {
        override fun project(projectId: String): Project {
            ensureProject(projectId)
            return Project(id = projectId)
        }

        override fun userProjects(auth: Auth) = projects.keys.map { Project(id = it) }

        override fun projectUsers(projectId: String): List<User> {
            ensureProject(projectId)
            return projects.getValue(projectId)
                    .map { User(projectId = projectId, id = it, status = "ACTIVATED") }
        }

        override fun ensureProject(projectId: String) {
            if (projectId !in projects) {
                throw HttpNotFoundException("project_not_found", "Project $projectId does not exist")
            }
        }

    }
}
