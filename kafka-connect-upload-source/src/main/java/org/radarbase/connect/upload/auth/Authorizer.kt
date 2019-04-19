package org.radarbase.connect.upload.auth

import okhttp3.*
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import java.io.IOException
import javax.ws.rs.NotAuthorizedException

interface Authorizer {
    fun accessToken(forceRefresh: Boolean = false): String
}

class ClientCredentialsAuthorizer(
        private val httpClient: OkHttpClient,
        private val clientId: String,
        private val clientSecret: String,
        private val scopes: List<String>,
        private val tokenUrl: String): Authorizer {


    override fun accessToken(forceRefresh: Boolean): String {

        // TODO store token state and add refresh token logic. involve forceRefresh into logic

        val form = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", clientId)
                .add("scope", scopes.joinToString(" "))
                .build()
        val request = Request.Builder()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", Credentials.basic(clientId, clientSecret))
                .addHeader("Content-type", "application/x-www-form-urlencoded")
                .url(tokenUrl)
                .post(form)
                .build()

        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseNode = UploadSourceConnectorConfig.mapper.readTree(response.body()?.charStream())
            return responseNode["access_token"]?.asText() ?: throw IOException("Cannot read access token from response")
        } else {
            throw NotAuthorizedException("Cannot get valid access token")
        }
    }
}
