package org.radarbase.connect.upload.auth

import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ClientCredentialsAuthorizerTest {
    private lateinit var clientCredentialsAuthorizer: ClientCredentialsAuthorizer

    private lateinit var httpClient: OkHttpClient

    @BeforeEach
    fun setUp() {

        httpClient = OkHttpClient()
        clientCredentialsAuthorizer = ClientCredentialsAuthorizer(
                httpClient,
                "radar_upload_connect",
                "upload_secret",
                "http://localhost:8090/managementportal/oauth/token",
                emptySet()
        )
    }

    @AfterEach
    fun tearDown() {

    }

    @Test
    fun getAccessToken() {
        val token = clientCredentialsAuthorizer.accessToken()
        assertNotNull(token)
    }

}
