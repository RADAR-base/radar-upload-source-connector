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

package org.radarbase.connect.upload.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.RecordDataDTO
import org.radarbase.connect.upload.api.RecordMetadataDTO
import org.radarbase.connect.upload.auth.ClientCredentialsAuthorizer
import org.radarbase.upload.Config
import org.radarbase.upload.api.SourceTypeDTO
import java.io.File
import java.net.URI
import java.time.LocalDateTime
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions
import org.mockito.Mockito
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.RecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.io.SftpFileUploaderTest
import org.radarbase.connect.upload.logging.RecordLogger
import org.radarcns.connector.upload.oxford.OxfordCameraImage
import java.time.Instant

class TestBase {
    companion object {
        const val baseUri = "http://0.0.0.0:8085/upload/api"

        const val tokenUrl = "http://localhost:8090/managementportal/oauth/token"

        const val sourceTypeName = "phone-acceleration"

        const val uploadConnectClient = "radar_upload_connect"

        const val uploadConnectSecret = "upload_secret"

        private const val BEARER = "Bearer "

        private const val USER = "sub-1"

        private const val PROJECT = "radar"

        private const val SOURCE = "03d28e5c-e005-46d4-a9b3-279c27fbbc83"

        private val APPLICATION_JSON = "application/json; charset=utf-8".toMediaType()

        val APPLICATION_ZIP = "application/zip".toMediaType()

        private val TEXT_CSV = "text/csv; charset=utf-8".toMediaType()

        val httpClient = OkHttpClient()


        private val sourceType = SourceTypeDTO(
                name = sourceTypeName,
                topics = mutableSetOf("test_topic"),
                contentTypes = mutableSetOf("application/text"),
                timeRequired = false,
                sourceIdRequired = false,
                configuration = mutableMapOf(
                    "setting1" to "value1",
                    "setting2" to "value2",
                ),
        )

        private val altoidaZip = SourceTypeDTO(
                name = "altoida",
                topics = mutableSetOf("test_topic"),
                contentTypes = mutableSetOf("application/zip"),
                timeRequired = false,
                sourceIdRequired = false,
                configuration = mutableMapOf(),
        )

        private val accelerationZip = SourceTypeDTO(
                name = "acceleration-zip",
                topics = mutableSetOf("test_topic_Acc"),
                contentTypes = mutableSetOf("application/zip"),
                timeRequired = false,
                sourceIdRequired = false,
                configuration = mutableMapOf(),
        )

        val uploadBackendConfig = Config(
                managementPortalUrl = "http://localhost:8090/managementportal/",
                clientId = "radar_upload_backend",
                clientSecret = "secret",
                baseUri = URI.create(baseUri),
                jdbcDriver = "org.postgresql.Driver",
                jdbcUrl = "jdbc:postgresql://localhost:5434/uploadconnector",
                jdbcUser = "radarcns",
                jdbcPassword = "radarcns",
                sourceTypes = listOf(
                    sourceType,
                    altoidaZip,
                    accelerationZip
                ),
        )


        val oxfordZipContents = ContentsDTO(
            contentType = "application/zip",
            fileName = "oxford-sample-data.zip",
            createdDate = Instant.now(),
            size = 1L,
        )

        val oxfordZipRecord = RecordDTO(
            id = 1L,
            metadata = RecordMetadataDTO(
                revision = 1,
                status = "PROCESSING",
            ),
            data = RecordDataDTO(
                projectId = "p",
                userId = "u",
                sourceId = "s",
            ),
            sourceType = "oxford-wearable-camera",
        )

        val oxfordZipSourceType = org.radarbase.connect.upload.api.SourceTypeDTO(
            name = "oxford-wearable-camera",
            configuration = emptyMap(),
            sourceIdRequired = false,
            timeRequired = false,
            topics = setOf(
                "connect_upload_oxford_camera_image",
                "connect_upload_oxford_camera_data",
            ),
            contentTypes = setOf("application/zip")
        )

        private val mapper: ObjectMapper = ObjectMapper(JsonFactory())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(KotlinModule())
                .registerModule(JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        val clientCredentialsAuthorizer = ClientCredentialsAuthorizer(
                httpClient,
                uploadConnectClient,
                uploadConnectSecret,
                tokenUrl)

        private fun call(
                httpClient: OkHttpClient,
                expectedStatus: Int,
                requestSupplier: (Request.Builder) -> Request.Builder
        ): JsonNode? {
            val request = requestSupplier(Request.Builder()).build()
            println(request.url)
            return httpClient.newCall(request).execute().use { response ->
                val body = response.body?.let {
                    val tree = mapper.readTree(it.byteStream())
                    println(tree)
                    tree
                }
                assertThat(response.code, `is`(expectedStatus))
                body
            }
        }

        fun<T> call(
                httpClient: OkHttpClient,
                expectedStatus: Int,
                parseClass: Class<T>,
                requestSupplier: Request.Builder.() -> Request.Builder
        ): T {
            val request = requestSupplier(Request.Builder()).build()
            println(request.url)
            return httpClient.newCall(request).execute().use { response ->
                assertThat(response.code, `is`(expectedStatus))
                assertThat(response.body, not(nullValue()))
                mapper.readValue(response.body?.byteStream(), parseClass)
                        .also { assertThat(it, not(nullValue())) }
                        .also { println(it!!.toJsonString()) }
            }
        }

        private fun Any.toJsonString(): String = mapper.writeValueAsString(this)

        private fun call(
                httpClient: OkHttpClient,
                expectedStatus: Response.Status,
                requestSupplier: (Request.Builder) -> Request.Builder
        ): JsonNode? = call(httpClient, expectedStatus.statusCode, requestSupplier)

        private fun call(
                httpClient: OkHttpClient,
                expectedStatus: Response.Status,
                stringProperty: String,
                requestSupplier: Request.Builder.() -> Request.Builder
        ): String = call(httpClient, expectedStatus, requestSupplier)
                ?.get(stringProperty)
                ?.asText()
                ?: throw AssertionError("String property $stringProperty not found")

        fun retrieveRecordMetadata(accessToken: String, recordId: Long): RecordMetadataDTO {
            return call(httpClient,200, RecordMetadataDTO::class.java) {
                url("$baseUri/records/$recordId/metadata")
                addHeader("Authorization", BEARER + accessToken)
            }
        }

        fun getAccessToken() : String = call(httpClient, Response.Status.OK, "access_token") {
            url(tokenUrl)
            addHeader("Authorization", Credentials.basic("radar_upload_test_client", "test"))
            post(FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .build())
        }

        fun createRecordAndUploadContent(accessToken: String, sourceType: String, fileName: String, type: okhttp3.MediaType = TEXT_CSV): RecordDTO {
            val recordCreated = createRecord(accessToken, sourceType)

            //Test uploading request contentFile for created record
            uploadContent(recordCreated.id!!, fileName, accessToken, type)
            markReady(recordCreated.id!!, accessToken)
            return recordCreated
        }

        fun createRecord(accessToken: String, sourceType: String): RecordDTO {
            val record = RecordDTO(
                    id = null,
                    data = RecordDataDTO(
                            projectId = PROJECT,
                            userId = USER,
                            sourceId = SOURCE,
                            time = LocalDateTime.now()
                    ),
                    sourceType = sourceType,
                    metadata = null
            )

            val recordCreated = call(httpClient, 201, RecordDTO::class.java) {
                url("$baseUri/records")
                post(record.toJsonString().toRequestBody(APPLICATION_JSON))
                addHeader("Authorization", BEARER + accessToken)
                addHeader("Content-Type", "application/json")
                addHeader("Content-Length", "10000")
            }
            assertThat(recordCreated.id, not(nullValue()))
            assertThat(recordCreated.id!!, greaterThan(0L))
            return recordCreated
        }

        fun uploadContent(recordId: Long, fileName: String, clientUserToken: String, type: okhttp3.MediaType = TEXT_CSV) {
            //Test uploading request contentFile
            val file = File(fileName)

            val content = call(httpClient, 201, ContentsDTO::class.java) {
                url("$baseUri/records/$recordId/contents/$fileName")
                put(file.asRequestBody(type))
                addHeader("Authorization", BEARER + clientUserToken)
                addHeader("Content-Length", "10000")
            }
            assertThat(content.fileName, equalTo(fileName))
        }

        fun markReady(recordId: Long, clientUserToken: String) {
            val metadata = call(httpClient, 200, RecordMetadataDTO::class.java) {
                url("$baseUri/records/$recordId/metadata")
                post("{\"status\":\"READY\",\"revision\":1}".toRequestBody("application/json".toMediaType()))
                addHeader("Authorization", BEARER + clientUserToken)
            }
            assertThat(metadata.status, equalTo("READY"))
        }

        fun readOxfordZip(converter: ConverterFactory.Converter): List<OxfordCameraImage> {
            val zipName = "oxford-camera-sample.zip"

            val records = mutableListOf<TopicData>()

            val context = ConverterFactory.ContentsContext.create(
                record = oxfordZipRecord,
                contents = oxfordZipContents,
                logger = Mockito.mock(RecordLogger::class.java),
                avroData = RecordConverter.createAvroData(),
            )
            requireNotNull(javaClass.getResourceAsStream(zipName)).use { zipStream ->
                converter.convertFile(context, zipStream, records::add)
            }
            Assertions.assertNotNull(oxfordZipRecord)
            Assertions.assertEquals(10, records.size) // 5 images, 5x upload + 5x metadata
            val imageRecords = records
                .filter { it.value is OxfordCameraImage }
                .map { it.value as OxfordCameraImage }
            Assertions.assertEquals(5, imageRecords.size)
            return imageRecords
        }
    }
}
