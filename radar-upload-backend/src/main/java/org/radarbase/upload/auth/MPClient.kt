package org.radarbase.upload.auth

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.radarbase.upload.Config
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.User
import org.radarbase.upload.exception.BadGatewayException
import java.net.MalformedURLException
import java.time.Duration
import java.time.Instant
import javax.ws.rs.core.Context

class MPClient(@Context config: Config, @Context private val auth: Auth) {
    private val clientId: String = config.clientId
    private val clientSecret: String = config.clientSecret ?: throw IllegalArgumentException("Cannot configure managementportal client without client secret")
    private val httpClient = OkHttpClient()
    private val baseUrl: HttpUrl = config.managementPortalUrl.toHttpUrlOrNull()
            ?: throw MalformedURLException("Cannot parse base URL ${config.managementPortalUrl} as an URL")
    private val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val projectListReader = mapper.readerFor(object : TypeReference<List<Project>>(){})
    private val userListReader = mapper.readerFor(object : TypeReference<List<SubjectDto>>(){})

    private var token: String? = null
    private var expiration: Instant? = null

    private val validToken: String?
        get() {
            val localToken = token ?: return null
            expiration?.takeIf { it > Instant.now() } ?: return null
            return localToken
        }

    private fun ensureToken(): String {
        var localToken = validToken

        return if (localToken != null) {
            localToken
        } else {
            val result = mapper.readTree(execute(Request.Builder().apply {
                url(baseUrl.resolve("oauth/token")!!)
                post(FormBody.Builder().apply {
                    add("grant_type", "client_credentials")
                    add("client_id", clientId)
                    add("client_secret", clientSecret)
                }.build())
                header("Authorization", Credentials.basic(clientId, clientSecret))
            }.build()))

            localToken = result["access_token"].asText()
                    ?: throw BadGatewayException("ManagementPortal did not provide an access token")
            expiration = Instant.now() + Duration.ofSeconds(result["expires_in"].asLong()) - Duration.ofMinutes(5)
            token = localToken
            localToken
        }
    }

    fun readProjects(): List<Project> {
        val request = Request.Builder().apply {
            url(baseUrl.resolve("api/projects")!!)
            header("Authorization", "Bearer ${ensureToken()}")
        }.build()

        return projectListReader.readValue(execute(request))
    }

    private fun execute(request: Request): String {
        return httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
               response.body?.string()
                       ?: throw BadGatewayException("ManagementPortal did not provide a result")
            } else {
                throw BadGatewayException("Cannot connect to managementportal")
            }
        }
    }

    fun readParticipants(projectId: String): List<User> {
        val request = Request.Builder().apply {
            url(baseUrl.resolve("api/projects/$projectId/users")!!)
            header("Authorization", "Bearer ${ensureToken()}")
        }.build()

        return userListReader.readValue<List<SubjectDto>>(execute(request))
                .map { User(it.login, projectId, it.externalId, it.status) }
    }

    data class SubjectDto(val login: String, val externalId: String? = null, val status: String = "DEACTIVATED", val attributes: Map<String, String> = mapOf())
}
