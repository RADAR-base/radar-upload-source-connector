package org.radarbase.connect.upload.api

import okhttp3.*
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.auth.Authorizer
import org.radarbase.connect.upload.exception.BadGatewayException
import org.radarbase.connect.upload.exception.NotAuthorizedException
import java.io.Closeable
import java.io.InputStream

class UploadBackendClient(
        private val auth: Authorizer,
        private var httpClient: OkHttpClient,
        private val uploadBackendBaseUrl: String): Closeable {

    init {
        httpClient = httpClient.newBuilder().authenticator {
            route, response -> response.request()
                .newBuilder()
                .header("Authorization", "Bearer ${auth.accessToken()}")
                .build()
        }.build()
    }

    fun pollRecords(configuration: PollDTO): RecordContainerDTO {
        val request = Request.Builder()
               .url(getUploadBackendUrl().plus("records/poll"))
               .post(RequestBody.create(APPLICATION_JSON, configuration.toJsonString()))
               .build()
        val response = httpClient.executeRequest(request)
        return  UploadSourceConnectorConfig.mapper.readValue(response.body()?.string(), RecordContainerDTO::class.java)
    }

    fun requestConnectorConfig(name: String): SourceTypeDTO {
        val request = Request.Builder()
                .url(getUploadBackendUrl().plus("source-types/").plus(name))
                .get()
                .build()
        val response = httpClient.executeRequest(request)
        return  UploadSourceConnectorConfig.mapper.readValue(response.body()?.string(), SourceTypeDTO::class.java)
    }

    fun requestAllConnectors(): SourceTypeContainerDTO {
        val request = Request.Builder()
                .url(getUploadBackendUrl().plus("source-types"))
                .get()
                .build()
        val response = httpClient.executeRequest(request)
        return UploadSourceConnectorConfig.mapper.readValue(response.body()?.string(), SourceTypeContainerDTO::class.java)
    }

    fun retrieveFile(record: RecordDTO, fileName: String): InputStream? {
        val request = Request.Builder()
                .url(getUploadBackendUrl().plus("records/${record.id}/contents/${fileName}"))
                .get()
                .build()
        val response = httpClient.executeRequest(request)
        return response.body()?.byteStream()
    }

    fun updateStatus(recordId: Long, newStatus: RecordMetadataDTO): RecordMetadataDTO {
        val request = Request.Builder()
                .url(getUploadBackendUrl().plus("records/$recordId/metadata"))
                .post(RequestBody.create(APPLICATION_JSON, newStatus.toJsonString()))
                .build()
        val response = httpClient.executeRequest(request)
        return  UploadSourceConnectorConfig.mapper.readValue(response.body()?.string(), RecordMetadataDTO::class.java)
    }

    fun addLogs(recordId: Long, status: RecordMetadataDTO): LogsDto {
        val request = Request.Builder()
                .url(getUploadBackendUrl().plus("records/$recordId/logs"))
                .post(RequestBody.create(APPLICATION_JSON, status.toJsonString()))
                .build()
        val response = httpClient.executeRequest(request)
        return  UploadSourceConnectorConfig.mapper.readValue(response.body()?.charStream(), LogsDto::class.java)
    }

    override fun close() {
    }

    private fun OkHttpClient.executeRequest(request: Request): Response {
        val response = this.newCall(request).execute()
        if (response.isSuccessful) {
            return response
        } else {
            when(response.code()) {
                401 -> throw NotAuthorizedException("access token is not provided or is invalid : ${response.message()}")
                403 -> throw NotAuthorizedException("access token is not authorized to perform this request")
            }
            throw BadGatewayException("Failed to make request to ${request.url()}: Error code ${response.code()}:  ${response.body()?.string()}")
        }

    }

    private fun getUploadBackendUrl(): String {
        if (this.uploadBackendBaseUrl.endsWith("/")) {
            return this.uploadBackendBaseUrl
        } else {
            return this.uploadBackendBaseUrl.plus("/")
        }
    }

    private fun Any.toJsonString(): String = UploadSourceConnectorConfig.mapper.writeValueAsString(this)

    companion object {
        val APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8")
    }

}
