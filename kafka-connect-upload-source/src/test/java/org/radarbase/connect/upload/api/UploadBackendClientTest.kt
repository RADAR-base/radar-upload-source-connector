package org.radarbase.connect.upload.api

import okhttp3.OkHttpClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.radarbase.connect.upload.auth.ClientCredentialsAuthorizer


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UploadBackendClientTest {
    private lateinit var clientCredentialsAuthorizer: ClientCredentialsAuthorizer

    private lateinit var httpClient: OkHttpClient

    private lateinit var uploadBackendClient: UploadBackendClient

    @BeforeAll
    fun setUp() {

        httpClient = OkHttpClient()
        clientCredentialsAuthorizer = ClientCredentialsAuthorizer(
                httpClient,
                "radar_upload_connect",
                "upload_secret",
                "http://localhost:8090/managementportal/oauth/token",
                emptySet()
        )
        uploadBackendClient = UploadBackendClient(
                clientCredentialsAuthorizer,
                httpClient,
                "http://0.0.0.0:8080/radar-upload/"
        )
    }

    @AfterEach
    fun tearDown() {

    }

    @Test
    fun requestAllConnectors() {
        val connectors = uploadBackendClient.requestAllConnectors()
        assertNotNull(connectors)
        assertTrue(!connectors.sourceTypes.isEmpty())
        assertNotNull(connectors.sourceTypes.find { it.name == "MyCSV" })
    }

    @Test
    fun requestConnectorConfigurations() {
        val connectors = uploadBackendClient.requestConnectorConfig("MyCSV")
        assertNotNull(connectors)
        assertEquals("MyCSV", connectors.name)
    }

    @Test
    fun pollRecords() {
        val pollConfig = PollDTO(
                limit = 10,
                supportedConverters = emptyList()
        )
        val connectors = uploadBackendClient.pollRecords(pollConfig)
        assertNotNull(connectors)
    }

}
