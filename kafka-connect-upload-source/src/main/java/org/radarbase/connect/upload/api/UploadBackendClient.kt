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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSink
import org.radarbase.connect.upload.exception.BadGatewayException
import org.radarbase.connect.upload.exception.ConflictException
import org.radarbase.connect.upload.exception.NotAuthorizedException
import org.radarbase.connect.upload.logging.Log
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException

open class UploadBackendClient(
    authInterceptor: Interceptor,
    private var httpClient: OkHttpClient,
    private var uploadBackendBaseUrl: String,
) : Closeable {

    init {
        httpClient = httpClient
            .newBuilder()
            .addInterceptor(authInterceptor)
            .build()

        uploadBackendBaseUrl = uploadBackendBaseUrl.trimEnd('/')
    }

    open fun pollRecords(configuration: PollDTO): RecordContainerDTO = httpClient.executeRequest {
        url("$uploadBackendBaseUrl/records/poll")
        post(configuration.toJsonBody())
    }

    open fun requestConnectorConfig(name: String): SourceTypeDTO = httpClient.executeRequest {
        url("$uploadBackendBaseUrl/source-types/$name")
    }

    open fun requestAllConnectors(): SourceTypeContainerDTO = httpClient.executeRequest {
        url("$uploadBackendBaseUrl/source-types")
    }

    open fun <T> retrieveFile(
        record: RecordDTO,
        fileName: String,
        handling: (ResponseBody) -> T,
    ): T {
        return httpClient.executeRequest({
            url("$uploadBackendBaseUrl/records/${record.id}/contents/$fileName")
        }) {
            handling(it.body ?: throw IOException("No file content response body"))
        }
    }

    open fun retrieveRecordMetadata(recordId: Long): RecordMetadataDTO = httpClient.executeRequest {
        url("$uploadBackendBaseUrl/records/$recordId/metadata")
    }

    open fun updateStatus(recordId: Long, newStatus: RecordMetadataDTO): RecordMetadataDTO {
        val result: RecordMetadataDTO = httpClient.executeRequest {
            url("$uploadBackendBaseUrl/records/$recordId/metadata")
            post(newStatus.toJsonBody())
        }
        logger.info(
            "Successfully updated record {} status to {} (rev. {})",
            recordId,
            result.status,
            result.revision,
        )
        return result
    }

    open fun addLogs(log: Log): RecordMetadataDTO = httpClient.executeRequest {
        url("$uploadBackendBaseUrl/records/${log.recordId}/logs")
        put(object : RequestBody() {
            override fun contentType() = TEXT_PLAIN

            override fun writeTo(sink: BufferedSink) = log.asString(sink)
        })
    }

    override fun close() {
    }

    private inline fun <reified T : Any> OkHttpClient.executeRequest(
        noinline requestBuilder: Request.Builder.() -> Request.Builder,
    ): T = executeRequest(requestBuilder) { response ->
        mapper.readValue(response.body?.byteStream(), T::class.java)
            ?: throw IOException("Received invalid response")
    }

    private fun <T> OkHttpClient.executeRequest(
        requestBuilder: Request.Builder.() -> Request.Builder,
        handling: (Response) -> T,
    ): T {
        val request = Request.Builder().requestBuilder().build()
        return this.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                logger.info("Request to ${request.url} is SUCCESSFUL")
                handling(response)
            } else {
                logger.info("Request to ${request.url} has FAILED with response-code ${response.code}")
                when (response.code) {
                    401 -> throw NotAuthorizedException("access token is not provided or is invalid : ${response.message}")
                    403 -> throw NotAuthorizedException("access token is not authorized to perform this request")
                    409 -> throw ConflictException("Conflicting request exception: ${response.message}")
                    else -> throw BadGatewayException("Failed to make request to ${request.url}: Error code ${response.code}:  ${response.body?.string()}")
                }
            }
        }
    }

    private fun Any.toJsonBody(mediaType: MediaType = APPLICATION_JSON): RequestBody = mapper
        .writeValueAsBytes(this)
        .toRequestBody(mediaType)

    companion object {
        private val logger = LoggerFactory.getLogger(UploadBackendClient::class.java)
        private val APPLICATION_JSON = "application/json; charset=utf-8".toMediaType()
        private val TEXT_PLAIN = "text/plain; charset=utf-8".toMediaType()
        private var mapper: ObjectMapper = jsonMapper {
            addModule(kotlinModule())
            addModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
}
