package org.radarbase.upload.inject

import org.radarbase.upload.Config
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.AuthValidator
import org.radarbase.upload.auth.ManagementPortalAuth
import org.radarcns.auth.authentication.TokenValidator
import org.radarcns.auth.config.TokenVerifierPublicKeyConfig
import org.slf4j.LoggerFactory
import java.net.URI
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Context

/** Creates a TokenValidator based on the current management portal configuration. */
class RadarTokenValidator constructor(@Context config: Config) : AuthValidator {
    private val tokenValidator = try {
        TokenValidator()
    } catch (e: RuntimeException) {
        TokenValidator(TokenVerifierPublicKeyConfig().apply {
            publicKeyEndpoints = listOf(URI("${config.managementPortalUrl}/oauth/token_key"))
            resourceName = config.jwtResourceName
        })
    }

    init {
        this.tokenValidator.refresh()
        logger.info("Refreshed Token Validator")
    }

    override fun verify(request: ContainerRequestContext): Auth? {
        return getToken(request)?.let {
            ManagementPortalAuth(tokenValidator.validateAccessToken(it))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RadarTokenValidator::class.java)
    }
}
