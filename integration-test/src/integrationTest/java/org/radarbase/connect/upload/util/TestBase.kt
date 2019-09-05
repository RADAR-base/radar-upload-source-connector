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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
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
import javax.ws.rs.core.Response

class TestBase {
    companion object {
        val fileAndSourceTypeStore = mapOf(
                "TEST_ACC.csv" to "phone-acceleration",
                "TEST_ACC.zip" to "acceleration-zip",
                "TEST_ZIP.zip" to "altoida-zip"
        )

        const val baseUri = "http://0.0.0.0:8080/radar-upload"

        const val tokenUrl = "http://localhost:8090/managementportal/oauth/token"

        const val sourceTypeName = "phone-acceleration"

        const val uploadConnectClient = "radar_upload_connect"

        const val uploadConnectSecret = "upload_secret"

        const val BEARER = "Bearer "

        const val USER = "sub-1"

        const val PROJECT = "radar"

        const val SOURCE = "03d28e5c-e005-46d4-a9b3-279c27fbbc83"

        val APPLICATION_JSON = "application/json; charset=utf-8".toMediaType()

        val TEXT_CSV = "text/csv; charset=utf-8".toMediaType()

        val httpClient = OkHttpClient()


        val sourceType = SourceTypeDTO(
                name = sourceTypeName,
                topics = mutableSetOf("test_topic"),
                contentTypes = mutableSetOf("application/text"),
                timeRequired = false,
                sourceIdRequired = false,
                configuration = mutableMapOf("setting1" to "value1", "setting2" to "value2")
        )

        val uploadBackendConfig = Config(
                managementPortalUrl = "http://localhost:8090/managementportal",
                baseUri = URI.create(baseUri),
                jdbcDriver = "org.postgresql.Driver",
                jdbcUrl = "jdbc:postgresql://localhost:5434/uploadconnector",
                jdbcUser = "radarcns",
                jdbcPassword = "radarcns",
                sourceTypes = listOf(sourceType)
        )

        val mapper = ObjectMapper(JsonFactory())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(KotlinModule())
                .registerModule(JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        val clientCredentialsAuthorizer = ClientCredentialsAuthorizer(
                httpClient,
                uploadConnectClient,
                uploadConnectSecret,
                tokenUrl
        )

        fun call(
                httpClient: OkHttpClient,
                expectedStatus: Response.Status,
                request: Request
        ): ResponseBody? {
            println(request.url)
            return httpClient.newCall(request).execute().use { response ->
                assertThat(response.code, CoreMatchers.`is`(expectedStatus.statusCode))
                response.body
            }
        }

        fun call(
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
                assertThat(response.code, CoreMatchers.`is`(expectedStatus))
                body
            }
        }

        fun Any.toJsonString(): String = mapper.writeValueAsString(this)

        fun call(
                httpClient: OkHttpClient,
                expectedStatus: Response.Status,
                requestSupplier: (Request.Builder) -> Request.Builder
        ): JsonNode? {
            return call(httpClient, expectedStatus.statusCode, requestSupplier)
        }

        fun call(
                httpClient: OkHttpClient,
                expectedStatus: Response.Status,
                stringProperty: String,
                requestSupplier: (Request.Builder) -> Request.Builder
        ): String {
            return call(httpClient, expectedStatus, requestSupplier)?.get(stringProperty)?.asText()
                    ?: throw AssertionError("String property $stringProperty not found")
        }


        fun createRecordAndUploadContent(sourceType: String, fileName: String) : RecordDTO {
            val clientUserToken = call(httpClient, Response.Status.OK, "access_token") {
                it.url(tokenUrl)
                        .addHeader("Authorization", Credentials.basic("radar_upload_test_client", "test"))
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
                    sourceType = sourceType,
                    metadata = null
            )

            val request = Request.Builder()
                    .url("$baseUri/records")
                    .post(record.toJsonString().toRequestBody(APPLICATION_JSON))
                    .addHeader("Authorization", BEARER + clientUserToken)
                    .addHeader("Content-type", "application/json")
                    .build()

            val response = httpClient.newCall(request).execute()
            Assertions.assertTrue(response.isSuccessful)

            val recordCreated = mapper.readValue(response.body?.string(), RecordDTO::class.java)
            Assertions.assertNotNull(recordCreated)
            Assertions.assertNotNull(recordCreated.id)
            assertThat(recordCreated?.id!!, Matchers.greaterThan(0L))

            //Test uploading request contentFile for created record
            uploadContent(recordCreated.id!!, fileName, clientUserToken)
            markReady(recordCreated.id!!, clientUserToken)
            return recordCreated
        }

        private fun uploadContent(recordId: Long, fileName: String, clientUserToken: String) {
            //Test uploading request contentFile
            val file = File(fileName)

            val requestToUploadFile = Request.Builder()
                    .url("$baseUri/records/$recordId/contents/$fileName")
                    .put(file.asRequestBody(TEXT_CSV))
                    .addHeader("Authorization", BEARER + clientUserToken)
                    .build()

            val uploadResponse = httpClient.newCall(requestToUploadFile).execute()
            Assertions.assertTrue(uploadResponse.isSuccessful)

            val content = mapper.readValue(uploadResponse.body?.string(), ContentsDTO::class.java)
            Assertions.assertNotNull(content)
            Assertions.assertEquals(fileName, content.fileName)
        }


        private fun markReady(recordId: Long, clientUserToken: String) {
            //Test uploading request contentFile
            val requestToUploadFile = Request.Builder()
                    .url("$baseUri/records/$recordId/metadata")
                    .post("{\"status\":\"READY\",\"revision\":1}".toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", BEARER + clientUserToken)
                    .build()

            val uploadResponse = httpClient.newCall(requestToUploadFile).execute()
            Assertions.assertTrue(uploadResponse.isSuccessful)

            val metadata = mapper.readValue(uploadResponse.body?.string(), RecordMetadataDTO::class.java)
            Assertions.assertNotNull(metadata)
            Assertions.assertEquals("READY", metadata.status)
        }

    }
}
