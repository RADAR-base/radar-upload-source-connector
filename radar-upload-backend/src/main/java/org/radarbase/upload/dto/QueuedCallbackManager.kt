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

package org.radarbase.upload.dto

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.radarbase.upload.api.RecordMetadataDTO
import org.radarbase.upload.logger
import java.io.IOException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import jakarta.ws.rs.core.Context

class QueuedCallbackManager(
        @Context val httpClient: OkHttpClient,
        @Context val scheduler: ScheduledExecutorService) : CallbackManager {

    override fun callback(metadata: RecordMetadataDTO, retries: Int) {
        metadata.callbackUrl?.let { url ->
            val request = Request.Builder()
                    .url(url)
                    .post(jsonWriter.writeValueAsString(metadata).toRequestBody(JSON_TYPE))
                    .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (it.isSuccessful) {
                            logger.debug("Successful callback {}", url)
                        } else {
                            val bodyString = if (it.body != null) it.peekBody(255).string() else "<empty>"

                            if (retries > 0) {
                                logger.debug("Callback to {} failed (code {}): {}, retrying", url, it.code, bodyString)
                                scheduler.schedule({ callback(metadata, retries - 1) }, 10, TimeUnit.MINUTES)
                            } else {
                                logger.debug("Callback to {} failed completely (code {}): {}", url, it.code, bodyString)
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
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
        private val jsonWriter = ObjectMapper().writerFor(RecordMetadataDTO::class.java)
    }
}
