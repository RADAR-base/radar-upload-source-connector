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

package org.radarbase.connect.upload.auth

import okhttp3.*
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.api.OauthToken
import org.radarbase.connect.upload.exception.NotAuthorizedException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.Exception
import java.time.Instant

class ClientCredentialsAuthorizer(
        private val httpClient: OkHttpClient,
        private val clientId: String,
        private val clientSecret: String,
        private val tokenUrl: String) : Authenticator {

    lateinit var token: OauthToken

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) {
            logger.debug("Received ${response.code} at the authenticator. Skipping this request...")
            return null
        }

        var accessToken = accessToken()
        if ("Bearer $accessToken" == response.request.header("Authorization")) {
            logger.debug("Request failed with token existing token. Requesting new token")
            Thread.sleep(60000L)
            accessToken = accessToken(true)
        }
        logger.debug("Response request ${response.request} and code ${response.code}")

        try {
            return response.request.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
        } catch (exe: Exception) {
            logger.info("Could not get a access token " , exe)
            throw NotAuthorizedException("Could not get a access token: ${exe.message}")
        }
    }

    fun accessToken(forceRefresh: Boolean = false): String {
        if (forceRefresh || !::token.isInitialized || token.isExpired) {
            this.token = requestAccessToken()
            logger.info("Token is initialized...")
        }

        return this.token.accessToken
    }

    private fun requestAccessToken(): OauthToken {
        val form = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build()

        val request = Request.Builder()
                .addHeader("Authorization", Credentials.basic(clientId.trim(), clientSecret.trim()))
                .url(tokenUrl)
                .post(form)
                .build()

        return httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                logger.info("Request to get access token was SUCCESSFUL")
                try {
                    UploadSourceConnectorConfig.mapper.readValue(response.body?.charStream(), OauthToken::class.java)
                } catch (exe: IOException) {
                    throw NotAuthorizedException("Could not convert response into a valid access token ${exe.message}")
                }
            } else {
                throw NotAuthorizedException("Request to get access token failed with response code ${response.code} and  ${response.body?.string()}")
            }
        }
    }


    private val OauthToken.isExpired: Boolean
        get() = Instant.now()
            .isAfter(Instant.ofEpochSecond(this.issuedAt + this.expiresIn))

    companion object {
        private val logger = LoggerFactory.getLogger(ClientCredentialsAuthorizer::class.java)
    }
}
