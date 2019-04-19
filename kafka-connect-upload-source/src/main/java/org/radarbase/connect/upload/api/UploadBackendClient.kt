package org.radarbase.connect.upload.api

import okhttp3.OkHttpClient
import okhttp3.Request
import org.radarbase.connect.upload.auth.Authorizer
import java.io.Closeable

class UploadBackendClient(
        private val auth: Authorizer,
        private val httpClient: OkHttpClient,
        private val baseUrl: String): Closeable {

    fun pollRecords(configuration: PollDTO): RecordContainerDTO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun requestConverterConfig(name: String): SourceTypeDTO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun requestAllConnectors(): SourceTypeContainerDTO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun retrieveFile(record: RecordDTO, fileName: String): RecordContainerDTO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun updateStatus(id: Long, newStatus: RecordMetadataDTO): RecordMetadataDTO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun addLogs(id: Long, status: RecordMetadataDTO): LogsDto {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun OkHttpClient.buildRequest(): Request {
        return Request.Builder()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", auth.accessToken())
                .addHeader("Content-type", "application/x-www-form-urlencoded")
                .build()
    }
}
