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

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import jakarta.ws.rs.core.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.radarbase.upload.api.RecordMetadataDTO
import org.radarbase.upload.logger
import java.util.concurrent.ScheduledExecutorService
import kotlin.time.Duration.Companion.minutes

class QueuedCallbackManager(
    @Context val scheduler: ScheduledExecutorService,
) : CallbackManager {

    val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
    }

    override fun callback(metadata: RecordMetadataDTO, retries: Int) {
        CoroutineScope(scheduler.asCoroutineDispatcher()).launch {
            doCallback(metadata, retries)
        }
    }

    private tailrec suspend fun doCallback(metadata: RecordMetadataDTO, retries: Int) {
        val url = metadata.callbackUrl ?: return

        val errorMessage = try {
            val response = withContext(Dispatchers.IO) {
                httpClient.post(url) {
                    setBody(metadata)
                    contentType(ContentType.Application.Json)
                }
            }

            if (response.status.isSuccess()) {
                logger.debug("Successful callback {}", url)
                return
            } else {
                val bodyString = if ((response.contentLength() ?: 0) > 0) response.bodyAsText().take(255) else "<empty>"
                "${response.status}: $bodyString"
            }
        } catch (ex: Exception) {
            ex.toString()
        }

        if (retries > 0) {
            logger.debug("Callback to {} failed: {}, retrying", url, errorMessage)
            delay(10.minutes)
            doCallback(metadata, retries - 1)
        } else {
            logger.debug("Callback to {} failed completely: {}", url, errorMessage)
        }
    }
}
