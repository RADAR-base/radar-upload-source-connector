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

package org.radarbase.connect.upload

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.radarbase.connect.upload.util.TestBase.Companion.baseUri
import org.radarbase.connect.upload.util.TestBase.Companion.createRecordAndUploadContent
import org.radarbase.connect.upload.util.TestBase.Companion.getAccessToken
import org.radarbase.connect.upload.util.TestBase.Companion.retrieveRecordMetadata
import org.radarbase.connect.upload.util.TestBase.Companion.tokenUrl
import org.radarbase.connect.upload.util.TestBase.Companion.uploadBackendConfig
import org.radarbase.connect.upload.util.TestBase.Companion.uploadConnectClient
import org.radarbase.connect.upload.util.TestBase.Companion.uploadConnectSecret
import org.radarbase.upload.Config
import org.radarbase.upload.GrizzlyServer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadSourceTaskTest {

    private lateinit var sourceTask: UploadSourceTask

    private lateinit var config: Config

    private lateinit var server: GrizzlyServer

    private lateinit var accessToken: String

    @BeforeAll
    fun setUp() {
        sourceTask = UploadSourceTask()

        config = uploadBackendConfig

        accessToken = getAccessToken()

        server = GrizzlyServer(config)
        server.start()

        val settings = mapOf(
                "upload.source.client.id" to uploadConnectClient,
                "upload.source.client.secret" to uploadConnectSecret,
                "upload.source.client.tokenUrl" to tokenUrl,
                "upload.source.backend.baseUrl" to baseUri,
                "upload.source.poll.interval.ms" to "10000",
                "upload.source.record.converter.classes" to listOf(
                        "org.radarbase.connect.upload.converter.AccelerometerCsvRecordConverter",
                        "org.radarbase.connect.upload.converter.altoida.AltoidaZipFileRecordConverter"
                ).joinToString(separator=",")
        )

        sourceTask.start(settings)
    }

    @AfterAll
    fun cleanUp() {
        server.shutdown()
        sourceTask.stop()
    }


    @Test
    @DisplayName("Should be able to convert a record with ZIP file")
    fun successfulZipFileConversion() {

        val sourceType = "altoida-zip"
        val fileName = "TEST_ZIP.zip"
        val createdRecord = createRecordAndUploadContent(accessToken, sourceType, fileName)
        assertNotNull(createdRecord)
        assertNotNull(createdRecord.id)

        val sourceRecords = sourceTask.poll()
        assertNotNull(sourceRecords)

        val metadata = retrieveRecordMetadata(accessToken, createdRecord.id!!)
        assertNotNull(metadata)
        assertEquals("PROCESSING", metadata.status)

        sourceRecords.forEach { sourceTask.commitRecord(it) }

        val metadataAfterCommit = retrieveRecordMetadata(accessToken, createdRecord.id!!)
        assertNotNull(metadataAfterCommit)
        assertEquals("SUCCEEDED", metadataAfterCommit.status)
    }

    @Test
    @DisplayName("Records of no registered converters should not be polled")
    fun noConverterFound() {
        val sourceType = "acceleration-zip"
        val fileName = "TEST_ACC.zip"
        val createdRecord = createRecordAndUploadContent(accessToken, sourceType, fileName)
        assertNotNull(createdRecord)
        assertNotNull(createdRecord.id)

        val sourceRecords = sourceTask.poll()
        assertNotNull(sourceRecords)
        assertTrue(sourceRecords.isEmpty())

        val metadata = retrieveRecordMetadata(accessToken, createdRecord.id!!)
        assertNotNull(metadata)
        assertEquals("READY", metadata.status)
    }

    @Test
    @DisplayName("Should mark FAILED if the record data does not match the source-type")
    fun incorrectSourceTypeForRecord() {
        val sourceType = "phone-acceleration"
        val fileName = "TEST_ACC.zip"
        val createdRecord = createRecordAndUploadContent(accessToken, sourceType, fileName)
        assertNotNull(createdRecord.id)

        val sourceRecords = sourceTask.poll()
        assertNotNull(sourceRecords)
        assertTrue(sourceRecords.isEmpty())

        val metadata = retrieveRecordMetadata(accessToken, createdRecord.id!!)
        assertNotNull(metadata)
        assertEquals("FAILED", metadata.status)
    }
}
