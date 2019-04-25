package org.radarbase.connect.upload.auth

import okhttp3.*
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.api.OauthToken
import org.radarbase.connect.upload.exception.NotAuthorizedException
import java.io.IOException
import java.time.Instant

interface Authorizer {
    fun accessToken(forceRefresh: Boolean = false): String
}

class ClientCredentialsAuthorizer(
        private val httpClient: OkHttpClient,
        private val clientId: String,
        private val clientSecret: String,
        private val tokenUrl: String,
        private val scopes: Set<String>?): Authorizer {

    lateinit var token: OauthToken

    override fun accessToken(forceRefresh: Boolean): String {

        if (forceRefresh || !::token.isInitialized || (::token.isInitialized && token.isExpired())) {
            this.token = requestAccessToken()
        }

        return this.token.accessToken
    }

    private fun requestAccessToken(): OauthToken {
        val form = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("scope", scopes?.joinToString { " " } ?: "")
                .build()

        val request = Request.Builder()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", Credentials.basic(clientId.trim(), clientSecret.trim()))
                .url(tokenUrl)
                .post(form)
                .build()

        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            try {
                return UploadSourceConnectorConfig.mapper.readValue(response.body()?.charStream(), OauthToken::class.java)
            } catch (exe: IOException) {
                throw NotAuthorizedException("Could not convert response into a valid access token ${exe.message}")
            }

        } else {
            throw NotAuthorizedException("Request to get access token failed with response code ${response.code()} and  ${response.body()?.string()}")
        }
    }


    private fun OauthToken.isExpired(): Boolean = Instant.now()
            .isAfter(Instant.ofEpochSecond(this.issuedAt + this.expiresIn))
}
