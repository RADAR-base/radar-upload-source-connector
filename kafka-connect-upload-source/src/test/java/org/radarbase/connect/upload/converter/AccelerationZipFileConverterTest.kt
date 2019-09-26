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

package org.radarbase.connect.upload.converter

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.radarbase.connect.upload.api.*
import java.io.File
import java.io.IOException
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccelerationZipFileConverterTest {

    private lateinit var converter: ZipFileRecordConverter

    private lateinit var logRepository: LogRepository

    private lateinit var uploadBackendClient: UploadBackendClient

    private val contentsDTO = ContentsDTO(
            contentType = "application/zip",
            fileName = "_ACC.zip",
            createdDate = Instant.now(),
            size = 1L
    )

    private val record = RecordDTO(
            id = 1L,
            metadata = RecordMetadataDTO(
                    revision = 1,
                    status = "PROCESSING"
            ),
            data = null,
            sourceType = "phone-acceleration"

    )
    private val timeReceived = Instant.now().toEpochMilli()

    @BeforeAll
    fun setUp() {
        uploadBackendClient = Mockito.mock(UploadBackendClient::class.java)
        logRepository = ConverterLogRepository(uploadBackendClient)
        converter = AccelerationZipFileConverter()
        converter.initialize(
                connectorConfig = SourceTypeDTO(
                        name = "phone-acceleration",
                        configuration = emptyMap(),
                        sourceIdRequired = false,
                        timeRequired = false,
                        topics = setOf("test_topic"),
                        contentTypes = setOf()
                ),
                client = uploadBackendClient,
                logRepository = logRepository,
                settings = emptyMap()
        )
    }


    @Test
    @DisplayName("Should be able to convert a zip file to TopicRecords")
    fun testValidDataProcessing() {
        val accFile = File("src/test/resources/_ACC.zip")

        val records = converter.processData(contentsDTO, accFile.inputStream(), record, timeReceived.toDouble())

        assertNotNull(record)
        assertEquals(6, records.size)
        assertEquals(true, records.last().endOfFileOffSet)
        assertEquals(1, records.filter { it.endOfFileOffSet }.size)
    }

    @Test
    @DisplayName("Should throw an IOException if the input is not a Zip file")
    fun testInValidDataProcessing() {
        val accFile = File("src/test/resources/ACC.csv")

        val exception = assertThrows(IOException::class.java) { converter.processData(contentsDTO, accFile.inputStream(), record, timeReceived.toDouble()) }
        assertNotNull(exception)
    }
}
