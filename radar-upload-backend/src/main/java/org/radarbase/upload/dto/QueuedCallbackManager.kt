package org.radarbase.upload.dto

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.*
import org.radarbase.upload.api.RecordMetadataDTO
import org.radarbase.upload.logger
import java.io.IOException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Context

class QueuedCallbackManager(
        @Context val httpClient: OkHttpClient,
        @Context val scheduler: ScheduledExecutorService): CallbackManager {

    override fun callback(metadata: RecordMetadataDTO, retries: Int) {
        metadata.callbackUrl?.let { url ->
            val request = Request.Builder()
                    .url(url)
                    .post(RequestBody.create(JSON_TYPE, jsonWriter.writeValueAsString(metadata)))
                    .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (it.isSuccessful) {
                            logger.debug("Successful callback {}", url)
                        } else {
                            val bodyString = if (it.body() != null) it.peekBody(255).string() else "<empty>"

                            if (retries > 0) {
                                logger.debug("Callback to {} failed (code {}): {}, retrying", url, it.code(), bodyString)
                                scheduler.schedule({ callback(metadata, retries - 1) }, 10, TimeUnit.MINUTES)
                            } else {
                                logger.debug("Callback to {} failed completely (code {}): {}", url, it.code(), bodyString)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (retries > 0) {
                        logger.debug("Callback to {} failed: {}, retrying", url, e.toString())
                        scheduler.schedule({ callback(metadata, retries - 1) }, 10, TimeUnit.MINUTES)
                    } else {
                        logger.debug("Callback to {} failed completely: {}", url, e.toString())
                    }
                }
            })
        }
    }

    companion object {
        private val JSON_TYPE = MediaType.parse("application/json; charset=utf-8")
        private val jsonWriter = ObjectMapper().writerFor(RecordMetadataDTO::class.java)
    }
}
