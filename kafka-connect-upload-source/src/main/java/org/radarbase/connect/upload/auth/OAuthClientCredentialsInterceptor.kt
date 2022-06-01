package org.radarbase.connect.upload.auth

import okhttp3.*
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.api.OAuthToken
import org.radarbase.connect.upload.exception.NotAuthorizedException
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.Throws

class OAuthClientCredentialsInterceptor(
    private val httpClient: OkHttpClient,
    private val clientId: String,
    private val clientSecret: String,
    private val tokenUrl: String,
) : Interceptor {
    private var token: OAuthToken? = null

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        //Build new request
        val accessToken = getAccessToken()
        val response: Response = chain.proceed(chain.request().withBearerAuth(accessToken))
        if (response.code == 401) { //if unauthorized
            //perform all 401 in sync blocks, to avoid multiply token updates
            logger.debug("Request failed with token existing token. Requesting new token")
            val refreshedAccessToken = getAccessToken(previousToken = accessToken)
            return chain.proceed(chain.request().withBearerAuth(refreshedAccessToken))
        }
        return response
    }

    @Synchronized
    fun getAccessToken(previousToken: String? = null): String {
        var token = this.token
        if (token == null || token.isExpired || previousToken == token.accessToken) {
            token = requestAccessToken()
                .also { this.token = it }
            logger.info("Token is initialized...")
        }

        return token.accessToken
    }


    private fun requestAccessToken(): OAuthToken {
        val request = Request.Builder().apply {
            header("Authorization", Credentials.basic(clientId.trim(), clientSecret.trim()))
            url(tokenUrl)
            post(FormBody.Builder().apply {
                add("grant_type", "client_credentials")
            }.build())
        }.build()

        return httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                logger.info("Request to get access token was SUCCESSFUL")
                try {
                    UploadSourceConnectorConfig.mapper.readValue(response.body?.charStream(), OAuthToken::class.java)
                } catch (exe: IOException) {
                    throw NotAuthorizedException("Could not convert response into a valid access token ${exe.message}")
                }
            } else {
                throw NotAuthorizedException("Request to get access token failed with response code ${response.code} and ${response.body?.string()}")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OAuthClientCredentialsInterceptor::class.java)

        private fun Request.withBearerAuth(token: String): Request = newBuilder().apply {
            header("Authorization", "Bearer $token")
        }.build()
    }
}
