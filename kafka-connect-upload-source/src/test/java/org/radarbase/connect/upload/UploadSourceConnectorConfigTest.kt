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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.radarbase.connect.upload.converter.altoida.AltoidaConverterFactory
import org.radarbase.connect.upload.converter.phone.AccelerometerConverterFactory

class UploadSourceConnectorConfigTest {

    @Test
    fun testConfig() {
        val settings = mapOf(
                "upload.source.client.id" to "radar_upload_connect",
                "upload.source.client.secret" to "test_secret",
                "upload.source.client.tokenUrl" to
                        "http://managementportal-app:8080/managementportal/oauth/token",
                "upload.source.backend.baseUrl" to
                        "http://radar-upload-connect-backend:8085/radar-upload/",
                "upload.source.poll.interval.ms" to "3600000",
                "upload.source.queue.size" to "1000",
                "upload.source.record.converter.classes" to listOf(
                        AccelerometerConverterFactory::class.java.name,
                        AltoidaConverterFactory::class.java.name
                ).joinToString(separator=",")
        )
        val config = UploadSourceConnectorConfig(settings)
        assertNotNull(config)

        assertEquals("radar_upload_connect", config.oauthClientId)
        assertEquals("test_secret", config.oauthClientSecret)
        assertEquals("http://managementportal-app:8080/managementportal/oauth/token", config.tokenRequestUrl)
        assertNotNull(config.authenticator)
        assertEquals(2, config.converterClasses.size)
    }
}
