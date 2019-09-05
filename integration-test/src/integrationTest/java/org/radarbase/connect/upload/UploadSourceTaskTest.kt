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

import org.junit.jupiter.api.*
import org.radarbase.connect.upload.util.TestBase.Companion.baseUri
import org.radarbase.connect.upload.util.TestBase.Companion.tokenUrl
import org.radarbase.connect.upload.util.TestBase.Companion.uploadBackendConfig
import org.radarbase.connect.upload.util.TestBase.Companion.uploadConnectClient
import org.radarbase.connect.upload.util.TestBase.Companion.uploadConnectSecret
import org.radarbase.upload.Config
import org.radarbase.upload.GrizzlyServer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadSourceTaskTest {

    private lateinit var sourceTask: UploadSourceTask

    private lateinit var config: Config

    private lateinit var server: GrizzlyServer

    @BeforeAll
    fun setUp() {
        sourceTask = UploadSourceTask()

        config = uploadBackendConfig

        server = GrizzlyServer(config)
        server.start()

    }

    @AfterAll
    fun cleanUp() {
        server.shutdown()
    }

    @Test
    @DisplayName("Should be able initialize all converters when starting")
    fun testSourceTaskStart() {

        val settings = mapOf(
                "upload.source.client.id" to uploadConnectClient,
                "upload.source.client.secret" to uploadConnectSecret,
                "upload.source.client.tokenUrl" to tokenUrl,
                "upload.source.backend.baseUrl" to baseUri,
                "upload.source.poll.interval.ms" to "3600000",
                "upload.source.record.converter.classes" to
                        "org.radarbase.connect.upload.converter.AccelerometerCsvRecordConverter"

        )

        sourceTask.start(settings)

    }
}
