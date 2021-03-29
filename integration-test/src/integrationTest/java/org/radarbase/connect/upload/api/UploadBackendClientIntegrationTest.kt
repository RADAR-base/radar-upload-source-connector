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

import org.apache.kafka.connect.source.SourceRecord
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.radarbase.connect.upload.converter.ConverterLogRepository
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.phone.AccelerometerConverterFactory
import org.radarbase.connect.upload.util.TestBase.Companion.baseUri
import org.radarbase.connect.upload.util.TestBase.Companion.clientCredentialsAuthorizer
import org.radarbase.connect.upload.util.TestBase.Companion.createRecordAndUploadContent
import org.radarbase.connect.upload.util.TestBase.Companion.getAccessToken
import org.radarbase.connect.upload.util.TestBase.Companion.httpClient
import org.radarbase.connect.upload.util.TestBase.Companion.sourceTypeName
import org.radarbase.connect.upload.util.TestBase.Companion.uploadBackendConfig
import org.radarbase.jersey.GrizzlyServer
import org.radarbase.jersey.config.ConfigLoader
import org.radarbase.upload.Config
import org.radarbase.upload.doa.entity.RecordStatus
import org.radarbase.upload.inject.ManagementPortalEnhancerFactory
import java.io.File


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadBackendClientIntegrationTest {
    private lateinit var uploadBackendClient: UploadBackendClient

    private lateinit var logRepository: LogRepository

    private lateinit var config: Config

    private lateinit var server: GrizzlyServer

    private lateinit var accessToken: String

    private val sourceType = sourceTypeName

    private val fileName = "TEST_ACC.csv"


    @BeforeAll
    fun setUp() {
        uploadBackendClient = UploadBackendClient(
                clientCredentialsAuthorizer,
                httpClient,
                baseUri
        )

        logRepository = ConverterLogRepository()

        accessToken = getAccessToken()

        config = uploadBackendConfig
        val resources = ConfigLoader.loadResources(ManagementPortalEnhancerFactory::class.java, config)

        server = GrizzlyServer(config.baseUri, resources)
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
        assertThat(connectors.sourceTypes, not(empty()))
        assertNotNull(connectors.sourceTypes.find { it.name == sourceType })
    }

    @Test
    fun requestConnectorConfigurations() {
        val connectors = uploadBackendClient.requestConnectorConfig(sourceType)
        assertNotNull(connectors)
        assertEquals(sourceType, connectors.name)
    }

    @Test
    fun testRecordCreationToConversionWorkFlow() {
        val createdRecord = createRecordAndUploadContent(accessToken, sourceType, fileName)
        retrieveFile(createdRecord)

        val records = pollRecords()

        val sourceType = uploadBackendClient.requestConnectorConfig(sourceType)

        val converter = AccelerometerConverterFactory()
                .converter(emptyMap(), sourceType, uploadBackendClient, logRepository)

        val recordToProcess = records.records.first { recordDTO -> recordDTO.sourceType == sourceTypeName }
        createdRecord.metadata = uploadBackendClient.updateStatus(recordToProcess.id!!, recordToProcess.metadata!!.copy(status = "PROCESSING", message = "The record is being processed"))
        val convertedRecords = mutableListOf<SourceRecord>()
        converter.convert(records.records.first(), convertedRecords::add)
        assertThat(convertedRecords, not(nullValue()))
        assertThat(convertedRecords, not(empty()))
    }

    private fun pollRecords(): RecordContainerDTO {
        val pollConfig = PollDTO(
                limit = 10,
                supportedConverters = setOf(sourceType))
        val records = uploadBackendClient.pollRecords(pollConfig)
        assertNotNull(records)
        assertThat(records.records.size, greaterThan(0))
        records.records.forEach { recordDTO ->
            assertThat(recordDTO.metadata?.status, equalTo(RecordStatus.QUEUED.toString()))
        }
        println("Polled ${records.records.size} records")
        return records
    }

    private fun retrieveFile(recordId: RecordDTO) {
        uploadBackendClient.retrieveFile(recordId, fileName) { response ->
            assertNotNull(response)
            val responseData = response.bytes()
            assertThat(responseData.size.toLong(), equalTo(File(fileName).length()))
            assertThat(responseData, equalTo(File(fileName).readBytes()))
        }
    }

}
