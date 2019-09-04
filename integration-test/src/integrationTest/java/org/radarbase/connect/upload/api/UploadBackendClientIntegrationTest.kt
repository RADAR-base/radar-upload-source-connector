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

package org.radarbase.connect.upload.api

import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.radarbase.connect.upload.auth.ClientCredentialsAuthorizer
import org.radarbase.connect.upload.converter.AccelerometerCsvRecordConverter
import org.radarbase.connect.upload.converter.ConverterLogRepository
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.util.TestUtils
import org.radarbase.connect.upload.util.TestUtils.Companion.APPLICATION_JSON
import org.radarbase.connect.upload.util.TestUtils.Companion.BEARER
import org.radarbase.connect.upload.util.TestUtils.Companion.PROJECT
import org.radarbase.connect.upload.util.TestUtils.Companion.SOURCE
import org.radarbase.connect.upload.util.TestUtils.Companion.TEXT_CSV
import org.radarbase.connect.upload.util.TestUtils.Companion.USER
import org.radarbase.connect.upload.util.TestUtils.Companion.call
import org.radarbase.connect.upload.util.TestUtils.Companion.fileName
import org.radarbase.connect.upload.util.TestUtils.Companion.mapper
import org.radarbase.connect.upload.util.TestUtils.Companion.toJsonString
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

    private lateinit var logRepository: LogRepository

    private lateinit var server: GrizzlyServer

    private lateinit var config: Config

    private val baseUri = TestUtils.baseUri

    private val mySourceTypeName = TestUtils.mySourceTypeName

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
        logRepository = ConverterLogRepository(uploadBackendClient)

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
    fun testRecordCreationToConvertionWorkFlow() {
        val clientUserToken = call(httpClient, Response.Status.OK, "access_token") {
            it.url("${config.managementPortalUrl}/oauth/token")
                    .addHeader("Authorization", Credentials.basic("radar_upload_connect", "upload_secret"))
                    .post(FormBody.Builder()
                            .add("grant_type", "client_credentials")
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
                .url("$baseUri/records")
                .post(record.toJsonString().toRequestBody(APPLICATION_JSON))
                .addHeader("Authorization", BEARER + clientUserToken)
                .addHeader("Content-type", "application/json")
                .build()

        val response = httpClient.newCall(request).execute()
        assertTrue(response.isSuccessful)

        val recordCreated = mapper.readValue(response.body?.string(), RecordDTO::class.java)
        assertNotNull(recordCreated)
        assertNotNull(recordCreated.id)
        assertThat(recordCreated?.id!!, greaterThan(0L))

        //Test uploading request contentFile for created record
        uploadContent(recordCreated.id!!, clientUserToken)
        markReady(recordCreated.id!!, clientUserToken)
        retrieveFile(recordCreated)

        val records = pollRecords()

        val sourceType = uploadBackendClient.requestConnectorConfig(mySourceTypeName)

        val converter = AccelerometerCsvRecordConverter()
        converter.initialize(sourceType, uploadBackendClient, logRepository, emptyMap())

        val recordToProcess = records.records.first()
        record.metadata = uploadBackendClient.updateStatus(recordToProcess.id!!, recordToProcess.metadata!!.copy(status = "PROCESSING", message = "The record is being processed"))
        val convertedRecords = converter.convert(records.records.first())
        assertNotNull(convertedRecords)
        assertNotNull(convertedRecords.result)
        assertTrue(convertedRecords.result?.isNotEmpty()!!)
        assertNotNull(convertedRecords.record)
        assertEquals(convertedRecords.record.id, recordToProcess.id!!)
    }

    private fun uploadContent(recordId: Long, clientUserToken: String) {
        //Test uploading request contentFile
        val file = File(fileName)

        val requestToUploadFile = Request.Builder()
                .url("$baseUri/records/$recordId/contents/$fileName")
                .put(file.asRequestBody(TEXT_CSV))
                .addHeader("Authorization", BEARER + clientUserToken)
                .build()

        val uploadResponse = httpClient.newCall(requestToUploadFile).execute()
        assertTrue(uploadResponse.isSuccessful)

        val content = mapper.readValue(uploadResponse.body?.string(), ContentsDTO::class.java)
        assertNotNull(content)
        assertEquals(fileName, content.fileName)
    }


    private fun markReady(recordId: Long, clientUserToken: String) {
        //Test uploading request contentFile
        val requestToUploadFile = Request.Builder()
                .url("$baseUri/records/$recordId/metadata")
                .post("{\"status\":\"READY\",\"revision\":1}".toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", BEARER + clientUserToken)
                .build()

        val uploadResponse = httpClient.newCall(requestToUploadFile).execute()
        assertTrue(uploadResponse.isSuccessful)

        val metadata = mapper.readValue(uploadResponse.body?.string(), RecordMetadataDTO::class.java)
        assertNotNull(metadata)
        assertEquals("READY", metadata.status)
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

    private fun retrieveFile(recordId: RecordDTO) {
        uploadBackendClient.retrieveFile(recordId, fileName).use { response ->
            assertNotNull(response)
            val responseData = response!!.bytes()
            assertThat(responseData.size.toLong(), equalTo(File(fileName).length()))
            assertThat(responseData, equalTo(File(fileName).readBytes()))
        }
    }

}
