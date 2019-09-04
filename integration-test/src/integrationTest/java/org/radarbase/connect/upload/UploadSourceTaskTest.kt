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

package org.radarbase.connect.upload

import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.auth.ClientCredentialsAuthorizer
import org.radarbase.connect.upload.converter.ConverterLogRepository
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.util.TestUtils
import org.radarbase.upload.Config
import org.radarbase.upload.GrizzlyServer
import org.radarbase.upload.api.SourceTypeDTO
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadSourceTaskTest {

    private lateinit var clientCredentialsAuthorizer: ClientCredentialsAuthorizer

    private lateinit var httpClient: OkHttpClient

    private lateinit var uploadBackendClient: UploadBackendClient

    private lateinit var logRepository: LogRepository

    private lateinit var server: GrizzlyServer

    private lateinit var config: Config

    @BeforeAll
    fun setUp() {

        httpClient = OkHttpClient()
        clientCredentialsAuthorizer = ClientCredentialsAuthorizer(
                httpClient,
                "radar_upload_connect",
                "upload_secret",
                "http://localhost:8090/managementportal/oauth/token",
                emptySet()
        )
        uploadBackendClient = UploadBackendClient(
                clientCredentialsAuthorizer,
                httpClient,
                TestUtils.baseUri
        )

        config = Config()
        config.managementPortalUrl = "http://localhost:8090/managementportal"
        config.baseUri = URI.create(TestUtils.baseUri)
        config.jdbcDriver = "org.postgresql.Driver"
        config.jdbcUrl = "jdbc:postgresql://localhost:5434/uploadconnector"
        config.jdbcUser = "radarcns"
        config.jdbcPassword = "radarcns"

        val sourceType = SourceTypeDTO(
                name = TestUtils.mySourceTypeName,
                topics = mutableSetOf("test_topic"),
                contentTypes = mutableSetOf("application/text"),
                timeRequired = false,
                sourceIdRequired = false,
                configuration = mutableMapOf("setting1" to "value1", "setting2" to "value2")
        )
        config.sourceTypes = listOf(sourceType)
        logRepository = ConverterLogRepository(uploadBackendClient)

        server = GrizzlyServer(config)
        server.start()

    }

    @AfterAll
    fun cleanUp() {
        server.shutdown()
    }

}
