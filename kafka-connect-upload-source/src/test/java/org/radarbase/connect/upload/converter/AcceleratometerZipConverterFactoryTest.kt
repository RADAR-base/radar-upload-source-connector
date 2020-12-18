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
import org.mockito.Mockito.mock
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.phone.AcceleratometerZipConverterFactory
import org.radarbase.connect.upload.exception.ConversionFailedException
import java.io.File
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AcceleratometerZipConverterFactoryTest {
    private lateinit var converter: ConverterFactory.Converter

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

    @BeforeAll
    fun setUp() {
        uploadBackendClient = mock(UploadBackendClient::class.java)
        logRepository = ConverterLogRepository()
        val converterFactory = AcceleratometerZipConverterFactory()
        val config = SourceTypeDTO(
                name = "phone-acceleration",
                configuration = emptyMap(),
                sourceIdRequired = false,
                timeRequired = false,
                topics = setOf("test_topic"),
                contentTypes = setOf()
        )
        converter = converterFactory.converter(emptyMap(), config, uploadBackendClient, logRepository)
    }


    @Test
    @DisplayName("Should be able to convert a zip file to TopicRecords")
    fun testValidDataProcessing() {
        val accFile = File("src/test/resources/_ACC.zip")

        val records = converter.convertFile(record, contentsDTO, accFile.inputStream(), mock(RecordLogger::class.java)).toList()

        assertNotNull(record)
        assertEquals(6, records.size)
        assertEquals(true, records.last().endOfFileOffSet)
        assertEquals(1, records.filter { it.endOfFileOffSet }.size)
    }

    @Test
    @DisplayName("Should throw an ConversionFailedException if the input is not a Zip file")
    fun testInValidDataProcessing() {
        val accFile = File("src/test/resources/ACC.csv")

        val exception = assertThrows(ConversionFailedException::class.java) {
            converter.convertFile(record, contentsDTO, accFile.inputStream(), mock(RecordLogger::class.java))
        }
        assertNotNull(exception)
    }
}
