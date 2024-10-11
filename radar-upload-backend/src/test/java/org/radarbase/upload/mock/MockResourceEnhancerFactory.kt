package org.radarbase.upload.mock

import jakarta.inject.Singleton
import jakarta.ws.rs.container.ContainerRequestContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.radarbase.auth.authorization.AuthorizationOracle
import org.radarbase.auth.authorization.EntityRelationService
import org.radarbase.auth.authorization.MPAuthorizationOracle
import org.radarbase.auth.token.RadarToken
import org.radarbase.jersey.auth.AuthConfig
import org.radarbase.jersey.auth.AuthValidator
import org.radarbase.jersey.enhancer.EnhancerFactory
import org.radarbase.jersey.enhancer.Enhancers
import org.radarbase.jersey.enhancer.JerseyResourceEnhancer
import org.radarbase.jersey.exception.HttpUnauthorizedException
import org.radarbase.jersey.hibernate.config.DatabaseConfig
import org.radarbase.jersey.hibernate.config.HibernateResourceEnhancer
import org.radarbase.jersey.service.ProjectService
import org.radarbase.jersey.service.managementportal.RadarProjectService
import org.radarbase.upload.Config
import org.radarbase.upload.inject.UploadResourceEnhancer
import java.util.Queue

class MockResourceEnhancerFactory(
    private val config: Config,
    private val authQueue: Queue<Pair<RadarToken?, Boolean>>,
    private val projects: Map<String, List<String>>,
    private val dbConfig: DatabaseConfig,
) : EnhancerFactory {
    override fun createEnhancers(): List<JerseyResourceEnhancer> {
        val projectService = MockProjectService(projects)
        return listOf(
            UploadResourceEnhancer(config),
            Enhancers.radar(
                AuthConfig(jwtResourceName = config.jwtResourceName),
            ),
            object : JerseyResourceEnhancer {
                override fun AbstractBinder.enhance() {
                    bind(projectService)
                        .to(RadarProjectService::class.java)
                        .`in`(Singleton::class.java)
                    bind(projectService)
                        .to(ProjectService::class.java)
                        .`in`(Singleton::class.java)

                    bind(
                        MPAuthorizationOracle(
                            object : EntityRelationService {
                                override suspend fun findOrganizationOfProject(project: String): String? {
                                    return if (project in projects) "main" else null
                                }
                            },
                        ),
                    )
                        .to(AuthorizationOracle::class.java)
                        .`in`(Singleton::class.java)

                    bind(MockAuthValidator())
                        .to(AuthValidator::class.java)
                }
            },
            HibernateResourceEnhancer(dbConfig),
            Enhancers.health,
            Enhancers.exception,
        )
    }

    private inner class MockAuthValidator : AuthValidator {
        override fun verify(token: String, request: ContainerRequestContext): RadarToken? {
            val element: Pair<RadarToken?, Boolean>? = authQueue.poll()
            MatcherAssert.assertThat("A next authentication attempt is expected", element, Matchers.not(Matchers.nullValue()))
            val (auth, isAuthorized) = requireNotNull(element)
            if (!isAuthorized) {
                throw HttpUnauthorizedException("token_missing", "Not authorized")
            }
            return auth
        }
    }
}
