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

package org.radarbase.upload.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.radarbase.upload.Config
import org.radarbase.upload.dto.Project
import org.radarbase.upload.dto.User
import org.radarbase.upload.exception.BadGatewayException
import org.slf4j.LoggerFactory
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
    private val projectListReader = mapper.readerFor(object : TypeReference<List<ProjectDto>>(){})
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
            val request = Request.Builder().apply {
                url(baseUrl.resolve("oauth/token")!!)
                post(FormBody.Builder().apply {
                    add("grant_type", "client_credentials")
                    add("client_id", clientId)
                    add("client_secret", clientSecret)
                }.build())
                header("Authorization", Credentials.basic(clientId, clientSecret))
            }.build()

            val result = mapper.readTree(execute(request))
            localToken = result["access_token"].asText()
                    ?: throw BadGatewayException("ManagementPortal did not provide an access token")
            expiration = Instant.now() + Duration.ofSeconds(result["expires_in"].asLong()) - Duration.ofMinutes(5)
            token = localToken
            localToken
        }
    }

    fun readProjects(): List<Project> {
        logger.debug("Requesting for projects")
        val request = Request.Builder().apply {
            url(baseUrl.resolve("api/projects")!!)
            header("Authorization", "Bearer ${ensureToken()}")
        }.build()

        return projectListReader.readValue<List<ProjectDto>>(execute(request))
                .map { Project(
                        id = it.id,
                        name = it.name,
                        location = it.location,
                        organization = it.organization,
                        description = it.description) }
    }

    private fun execute(request: Request): String {
        return httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
               response.body?.string()
                       ?: throw BadGatewayException("ManagementPortal did not provide a result")
            } else {
                logger.error("Cannot connect to managementportal ", response.code)
                throw BadGatewayException("Cannot connect to managementportal")
            }
        }
    }

    fun readParticipants(projectId: String): List<User> {
        val request = Request.Builder().apply {
            url(baseUrl.resolve("api/projects/$projectId/subjects")!!)
            header("Authorization", "Bearer ${ensureToken()}")
        }.build()

        return userListReader.readValue<List<SubjectDto>>(execute(request))
                .map { User(
                        id = it.login,
                        projectId = projectId,
                        externalId = it.externalId,
                        status = it.status) }
    }

    data class SubjectDto(val login: String, val externalId: String? = null, val status: String = "DEACTIVATED", val attributes: Map<String, String> = mapOf())

    data class ProjectDto(@JsonProperty("projectName") val id: String, @JsonProperty("humanReadableProjectName") val name: String? = null, val location: String? = null, val organization: String? = null, val description: String? = null)

    companion object {
        private val logger = LoggerFactory.getLogger(MPClient::class.java)
    }
}
