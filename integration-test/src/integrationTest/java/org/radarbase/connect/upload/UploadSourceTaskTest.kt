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

import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import org.apache.kafka.connect.source.SourceRecord
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.altoida.AltoidaConverterFactory
import org.radarbase.connect.upload.converter.phone.AccelerometerConverterFactory
import org.radarbase.connect.upload.util.TestBase.Companion.APPLICATION_ZIP
import org.radarbase.connect.upload.util.TestBase.Companion.baseUri
import org.radarbase.connect.upload.util.TestBase.Companion.createRecord
import org.radarbase.connect.upload.util.TestBase.Companion.createRecordAndUploadContent
import org.radarbase.connect.upload.util.TestBase.Companion.getAccessToken
import org.radarbase.connect.upload.util.TestBase.Companion.httpClient
import org.radarbase.connect.upload.util.TestBase.Companion.markReady
import org.radarbase.connect.upload.util.TestBase.Companion.retrieveRecordMetadata
import org.radarbase.connect.upload.util.TestBase.Companion.tokenUrl
import org.radarbase.connect.upload.util.TestBase.Companion.uploadBackendConfig
import org.radarbase.connect.upload.util.TestBase.Companion.uploadConnectClient
import org.radarbase.connect.upload.util.TestBase.Companion.uploadConnectSecret
import org.radarbase.connect.upload.util.TestBase.Companion.uploadContent
import org.radarbase.jersey.GrizzlyServer
import org.radarbase.jersey.config.ConfigLoader
import org.radarbase.upload.inject.ManagementPortalEnhancerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadSourceTaskTest {

    private lateinit var sourceTask: UploadSourceTask

    private lateinit var server: GrizzlyServer

    private lateinit var accessToken: String

    @BeforeAll
    fun setUp() {
        sourceTask = UploadSourceTask()

        val config = uploadBackendConfig

        val resources = ConfigLoader.loadResources(ManagementPortalEnhancerFactory::class.java, config)

        accessToken = getAccessToken()

        server = GrizzlyServer(config.baseUri, resources)
        server.start()

        val settings = mapOf(
            "upload.source.client.id" to uploadConnectClient,
            "upload.source.client.secret" to uploadConnectSecret,
            "upload.source.client.tokenUrl" to tokenUrl,
            "upload.source.backend.baseUrl" to baseUri,
            "upload.source.poll.interval.ms" to "2000",
            "upload.source.queue.size" to "1000",
            "upload.source.record.converter.classes" to listOf(
                    AccelerometerConverterFactory::class.java.name,
                    AltoidaConverterFactory::class.java.name
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
    @Timeout(value = 8, unit = TimeUnit.SECONDS)
    @DisplayName("Should be able to convert a record with ZIP file")
    fun successfulZipFileConversion() {
        val sourceType = "altoida"
        val fileName = "TEST_ZIP.zip"
        val createdRecord = createRecordAndUploadContent(accessToken, sourceType, fileName, APPLICATION_ZIP)
        assertNotNull(createdRecord)
        assertNotNull(createdRecord.id)

        val sourceRecords = mutableListOf<SourceRecord>()

        Thread.sleep(4_000L)
        while (!sourceRecords.containsEndOfRecord()) {
            val newRecords = sourceTask.poll()
            if (newRecords != null) {
                sourceRecords += newRecords
            }
        }

        val metadata = retrieveRecordMetadata(accessToken, createdRecord.id!!)
        assertNotNull(metadata)
        assertEquals("PROCESSING", metadata.status)

        sourceRecords.forEach { sourceTask.commitRecord(it) }

        val metadataAfterCommit = retrieveRecordMetadata(accessToken, createdRecord.id!!)
        assertNotNull(metadataAfterCommit)
        assertEquals("SUCCEEDED", metadataAfterCommit.status)
    }

    private fun List<SourceRecord>.containsEndOfRecord(): Boolean {
        val last = lastOrNull() ?: return false
        return last.sourceOffset()[END_OF_RECORD_KEY] == true
    }

    @Test
    @DisplayName("Records of no registered converters should not be polled")
    fun noConverterFound() {
        val sourceType = "acceleration-zip"
        val fileName = "TEST_ACC.zip"
        val createdRecord = createRecordAndUploadContent(accessToken, sourceType, fileName, APPLICATION_ZIP)
        assertNotNull(createdRecord)
        assertNotNull(createdRecord.id)

        Thread.sleep(4_000L)
        val sourceRecords = sourceTask.poll()
        assertThat(sourceRecords, nullValue())

        val metadata = retrieveRecordMetadata(accessToken, createdRecord.id!!)
        assertNotNull(metadata)
        assertEquals("READY", metadata.status)
    }

    @Test
    @DisplayName("Should mark FAILED if the record data does not match the source-type")
    fun incorrectSourceTypeForRecord() {
        val sourceType = "phone-acceleration"
        val fileName = "TEST_ACC.zip"
        val createdRecord = createRecordAndUploadContent(accessToken, sourceType, fileName, APPLICATION_ZIP)
        assertNotNull(createdRecord.id)

        Thread.sleep(4_000L)
        val sourceRecords: List<SourceRecord>? = sourceTask.poll()
        assertThat(sourceRecords, nullValue())

        val metadata = retrieveRecordMetadata(accessToken, createdRecord.id!!)
        assertNotNull(metadata)
        assertEquals("FAILED", metadata.status)
    }

    @Test
    @DisplayName("Should recuperate from failed upload")
    fun failedUpload() {
        val sourceType = "phone-acceleration"
        val fileName = "TEST_ACC.zip"

        val record = createRecord(accessToken, sourceType)

        class CancelableRequestBody : RequestBody() {
            var call: Call? = null
            var repetitions = 1
            override fun contentType() = "application-octet/stream".toMediaType()
            override fun writeTo(sink: BufferedSink) {
                repeat(100) { i ->
                    sink.write(ByteArray(1_000_000))
                    sink.flush()
                    Thread.sleep(10)
                    call?.takeIf { i >= repetitions }?.cancel()
                }
            }
        }

        val body = CancelableRequestBody()

        val request = okhttp3.Request.Builder()
                .url("$baseUri/records/${record.id}/contents/$fileName")
                .header("Authorization", "Bearer $accessToken")
                .put(body)
                .build()

        val call = httpClient.newCall(request)
        body.call = call
        body.repetitions = 50
        assertThrows(IOException::class.java) { call.execute() }

        uploadContent(record.id!!, fileName, accessToken, APPLICATION_ZIP)
        markReady(record.id!!, accessToken)
    }
}
