package org.radarbase.connect.upload.api

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.*
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.radarbase.connect.upload.auth.ClientCredentialsAuthorizer
import org.radarbase.upload.Config
import org.radarbase.upload.GrizzlyServer
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.doa.entity.RecordStatus
import java.io.File
import java.net.URI
import java.time.LocalDateTime
import javax.ws.rs.core.Response

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadBackendClientIntegrationTest {

    private lateinit var clientCredentialsAuthorizer: ClientCredentialsAuthorizer

    private lateinit var httpClient: OkHttpClient

    private lateinit var uploadBackendClient: UploadBackendClient

    private lateinit var server: GrizzlyServer

    private lateinit var config: Config

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
                baseUri
        )

        config = Config()
        config.managementPortalUrl = "http://localhost:8090/managementportal"
        config.baseUri = URI.create(baseUri)
        config.jdbcDriver = "org.postgresql.Driver"
        config.jdbcUrl = "jdbc:postgresql://localhost:5434/uploadconnector"
        config.jdbcUser = "radarcns"
        config.jdbcPassword = "radarcns"

        val sourceType = SourceTypeDTO(
                name = mySourceTypeName,
                topics = mutableSetOf("test_topic"),
                contentTypes = mutableSetOf("application/text"),
                timeRequired = false,
                sourceIdRequired = false,
                configuration = mutableMapOf("setting1" to "value1", "setting2" to "value2")
        )
        config.sourceTypes = listOf(sourceType)

        server = GrizzlyServer(config)
        server.start()

    }

    @AfterAll
    fun cleanUp() {
        server.shutdown()
    }

    @Test
    fun requestAllConnectors() {
        val connectors = uploadBackendClient.requestAllConnectors()
        assertNotNull(connectors)
        assertTrue(!connectors.sourceTypes.isEmpty())
        assertNotNull(connectors.sourceTypes.find { it.name == mySourceTypeName })
    }

    @Test
    fun requestConnectorConfigurations() {
        val connectors = uploadBackendClient.requestConnectorConfig(mySourceTypeName)
        assertNotNull(connectors)
        assertEquals(mySourceTypeName, connectors.name)
    }

    @Test
    fun requestToCreateAndPollRecord() {
        val clientUserToken = call(httpClient, Response.Status.OK, "access_token") {
            it.url("${config.managementPortalUrl}/oauth/token")
                    .addHeader("Authorization", Credentials.basic(REST_UPLOAD_CLIENT, REST_UPLOAD_SECRET))
                    .post(FormBody.Builder()
                            .add("username", ADMIN_USER)
                            .add("password", ADMIN_PASSWORD)
                            .add("grant_type", "password")
                            .build())
        }

        val record = RecordDTO(
                id = null,
                data = RecordDataDTO(
                        projectId = PROJECT,
                        userId = USER,
                        sourceId = SOURCE,
                        time = LocalDateTime.now()

                ),
                sourceType = mySourceTypeName,
                metadata = null
        )

        val request = Request.Builder()
                .url(baseUri.plus("records"))
                .post(RequestBody.create(APPLICATION_JSON, record.toJsonString()))
                .addHeader("Authorization", BEARER + clientUserToken)
                .addHeader("Content-type", "application/json")
                .build()

        val response = httpClient.newCall(request).execute()
        assertTrue(response.isSuccessful)

        val recordCreated =  mapper.readValue(response.body()?.string(), RecordDTO::class.java)
        assertNotNull(recordCreated)
        assertNotNull(recordCreated.id)
        assertThat(recordCreated?.id!!, greaterThan(0L))

        //Test uploading request content for created record
        uploadContent(recordCreated.id!!, clientUserToken)

        val records = pollRecords()
    }

    private fun uploadContent(recordId: Long, clientUserToken: String) {
        //Test uploading request content
        val file = File(fileName)

        val requestToUploadFile = Request.Builder()
                .url(baseUri.plus("records/$recordId/contents/$fileName"))
                .put(RequestBody.create(TEXT_CSV, file))
                .addHeader("Authorization", BEARER + clientUserToken)
                .build()

        val uploadResponse = httpClient.newCall(requestToUploadFile).execute()
        assertTrue(uploadResponse.isSuccessful)

        val content = mapper.readValue(uploadResponse.body()?.string(), ContentsDTO::class.java)
        assertNotNull(content)
        assertEquals(fileName, content.fileName)
    }

    fun pollRecords(): RecordContainerDTO {
        val pollConfig = PollDTO(
                limit = 10,
                supportedConverters = emptyList()
        )
        val records = uploadBackendClient.pollRecords(pollConfig)
        assertNotNull(records)
        assertThat(records.records.size, greaterThan(0))
        records.records.map { recordDTO -> assertEquals(RecordStatus.QUEUED.toString(), recordDTO.metadata?.status) }
        println("Polled ${records.records.size} records")
        return records
    }

    private fun retrieveFile() {

    }


    companion object {
        const val fileName = "TEST_ACC.csv"
        const val REST_UPLOAD_CLIENT = "radar_upload_backend"
        const val REST_UPLOAD_SECRET = "secret"
        const val USER = "sub-1"
        const val PROJECT = "radar"
        const val SOURCE = "03d28e5c-e005-46d4-a9b3-279c27fbbc83"
        const val ADMIN_USER = "admin"
        const val ADMIN_PASSWORD = "admin"
        private val factory = JsonFactory()
        private val mapper = ObjectMapper(factory)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(KotlinModule())
                .registerModule(JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        private val baseUri = "http://0.0.0.0:8080/radar-upload/"
        private val mySourceTypeName = "phone-acceleration"
        private const val BEARER = "Bearer "
        private val APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8")
        private val TEXT_CSV = MediaType.parse("text/csv; charset=utf-8")

        fun call(httpClient: OkHttpClient, expectedStatus: Response.Status, request: Request): ResponseBody? {
            println(request.url())
            return httpClient.newCall(request).execute().use { response ->
                assertThat(response.code(), CoreMatchers.`is`(expectedStatus.statusCode))
                response.body()
            }
        }

        fun call(httpClient: OkHttpClient, expectedStatus: Int, requestSupplier: (Request.Builder) -> Request.Builder): JsonNode? {
            val request = requestSupplier(Request.Builder()).build()
            println(request.url())
            return httpClient.newCall(request).execute().use { response ->
                val body = response.body()?.let {
                    val tree = mapper.readTree(it.byteStream())
                    println(tree)
                    tree
                }
                assertThat(response.code(), CoreMatchers.`is`(expectedStatus))
                body
            }
        }

        private fun Any.toJsonString(): String = mapper.writeValueAsString(this)

        fun call(httpClient: OkHttpClient, expectedStatus: Response.Status, requestSupplier: (Request.Builder) -> Request.Builder): JsonNode? {
            return call(httpClient, expectedStatus.statusCode, requestSupplier)
        }

        fun call(httpClient: OkHttpClient, expectedStatus: Response.Status, stringProperty: String, requestSupplier: (Request.Builder) -> Request.Builder): String {
            return call(httpClient, expectedStatus, requestSupplier)?.get(stringProperty)?.asText()
                    ?: throw AssertionError("String property $stringProperty not found")
        }
    }
}
