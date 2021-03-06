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

package org.radarbase.connect.upload.auth

import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.radarbase.connect.upload.util.TestBase.Companion.tokenUrl
import org.radarbase.connect.upload.util.TestBase.Companion.uploadConnectClient
import org.radarbase.connect.upload.util.TestBase.Companion.uploadConnectSecret

internal class ClientCredentialsAuthorizerTest {
    private lateinit var clientCredentialsAuthorizer: ClientCredentialsAuthorizer

    private lateinit var httpClient: OkHttpClient

    @BeforeEach
    fun setUp() {
        httpClient = OkHttpClient()
        clientCredentialsAuthorizer = ClientCredentialsAuthorizer(
                httpClient,
                uploadConnectClient,
                uploadConnectSecret,
                tokenUrl
        )
    }

    @Test
    fun getAccessToken() {
        val token = clientCredentialsAuthorizer.accessToken()
        assertNotNull(token)
    }

}
