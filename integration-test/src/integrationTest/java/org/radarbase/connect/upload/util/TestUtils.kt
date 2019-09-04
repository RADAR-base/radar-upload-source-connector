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
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.ResponseBody
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.radarbase.connect.upload.auth.ClientCredentialsAuthorizer
import javax.ws.rs.core.Response

class TestUtils {
    companion object {
        val fileAndSourceTypeStore = mapOf(
                "TEST_ACC.csv" to "phone-acceleration",
                "TEST_ACC.zip" to "acceleration-zip",
                "TEST_ZIP.zip" to "altoida-zip"
        )
        const val fileName = "TEST_ACC.csv"
        const val REST_UPLOAD_CLIENT = "radar_upload_backend"
        const val REST_UPLOAD_SECRET = "secret"
        const val USER = "sub-1"
        const val PROJECT = "radar"
        const val SOURCE = "03d28e5c-e005-46d4-a9b3-279c27fbbc83"
        const val ADMIN_USER = "admin"
        const val ADMIN_PASSWORD = "admin"
        val mapper = ObjectMapper(JsonFactory())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(KotlinModule())
                .registerModule(JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        const val baseUri = "http://0.0.0.0:8080/radar-upload"
        const val mySourceTypeName = "phone-acceleration"
        const val BEARER = "Bearer "
        val APPLICATION_JSON = "application/json; charset=utf-8".toMediaType()
        val TEXT_CSV = "text/csv; charset=utf-8".toMediaType()

        val httpClient = OkHttpClient()

        val clientCredentialsAuthorizer = ClientCredentialsAuthorizer(
                httpClient,
                "radar_upload_connect",
                "upload_secret",
                "http://localhost:8090/managementportal/oauth/token",
                emptySet()
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
    }
}
