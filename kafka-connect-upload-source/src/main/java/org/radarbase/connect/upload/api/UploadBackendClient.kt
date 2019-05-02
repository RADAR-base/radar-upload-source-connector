package org.radarbase.connect.upload.api

import okhttp3.*
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.auth.Authorizer
import org.radarbase.connect.upload.exception.BadGatewayException
import org.radarbase.connect.upload.exception.NotAuthorizedException
import java.io.Closeable

class UploadBackendClient(
        private val auth: Authorizer,
        private var httpClient: OkHttpClient,
        private var uploadBackendBaseUrl: String) : Closeable {

    init {
        httpClient = httpClient.newBuilder().authenticator { route, response ->
            response.request()
                    .newBuilder()
                    .header("Authorization", "Bearer ${auth.accessToken()}")
                    .build()
        }.build()

        if (!this.uploadBackendBaseUrl.endsWith("/")) {
            this.uploadBackendBaseUrl += "/"
        }
    }

    fun pollRecords(configuration: PollDTO): RecordContainerDTO {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("records/poll"))
                .post(RequestBody.create(APPLICATION_JSON, configuration.toJsonString()))
                .build()
        val response = httpClient.executeRequest(request)
        return UploadSourceConnectorConfig.mapper.readValue(response.body()?.string(), RecordContainerDTO::class.java)
    }

    fun requestConnectorConfig(name: String): SourceTypeDTO {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("source-types/").plus(name))
                .get()
                .build()
        val response = httpClient.executeRequest(request)
        return UploadSourceConnectorConfig.mapper.readValue(response.body()?.string(), SourceTypeDTO::class.java)
    }

    fun requestAllConnectors(): SourceTypeContainerDTO {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("source-types"))
                .get()
                .build()
        val response = httpClient.executeRequest(request)
        return UploadSourceConnectorConfig.mapper.readValue(response.body()?.string(), SourceTypeContainerDTO::class.java)
    }

    fun retrieveFile(record: RecordDTO, fileName: String): ResponseBody? {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("records/${record.id}/contents/${fileName}"))
                .get()
                .build()
        val response = httpClient.executeRequest(request)
        return response.body()
    }

    fun updateStatus(recordId: Long, newStatus: RecordMetadataDTO): RecordMetadataDTO {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("records/$recordId/metadata"))
                .post(RequestBody.create(APPLICATION_JSON, newStatus.toJsonString()))
                .build()
        val response = httpClient.executeRequest(request)
        return UploadSourceConnectorConfig.mapper.readValue(response.body()?.string(), RecordMetadataDTO::class.java)
    }

    fun addLogs(recordId: Long, status: RecordMetadataDTO): LogsDto {
        val request = Request.Builder()
                .url(uploadBackendBaseUrl.plus("records/$recordId/logs"))
                .post(RequestBody.create(APPLICATION_JSON, status.toJsonString()))
                .build()
        val response = httpClient.executeRequest(request)
        return UploadSourceConnectorConfig.mapper.readValue(response.body()?.charStream(), LogsDto::class.java)
    }

    override fun close() {
    }

    private fun OkHttpClient.executeRequest(request: Request): Response {
        val response = this.newCall(request).execute()
        if (response.isSuccessful) {
            return response
        } else {
            when (response.code()) {
                401 -> throw NotAuthorizedException("access token is not provided or is invalid : ${response.message()}")
                403 -> throw NotAuthorizedException("access token is not authorized to perform this request")
            }
            throw BadGatewayException("Failed to make request to ${request.url()}: Error code ${response.code()}:  ${response.body()?.string()}")
        }

    }

    private fun Any.toJsonString(): String = UploadSourceConnectorConfig.mapper.writeValueAsString(this)

    companion object {
        val APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8")
    }

}
