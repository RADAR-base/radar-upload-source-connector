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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.phone.AccelerometerConverterFactory
import org.radarbase.connect.upload.exception.ConversionFailedException
import java.io.File
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccelerometerCsvRecordConverterTest {

    private lateinit var converter: ConverterFactory.Converter

    private lateinit var logRepository: LogRepository

    private val contentsDTO = ContentsDTO(
            contentType = "application/csv",
            fileName = "ACC_INVALID.CSV",
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
        logRepository = ConverterLogRepository()
        val converterFactory = AccelerometerConverterFactory()
        converter = converterFactory.converter(
                client = mock(UploadBackendClient::class.java),
                connectorConfig = SourceTypeDTO(
                        name = "phone-acceleration",
                        configuration = emptyMap(),
                        sourceIdRequired = false,
                        timeRequired = false,
                        topics = setOf("test_topic"),
                        contentTypes = setOf()
                ),
                logRepository = logRepository,
                settings = emptyMap()
        )
    }


    @Test
    @DisplayName("Should be able to convert ACC.csv to TopicRecords")
    fun testValidDataProcess() {
        val accFile = File("src/test/resources/ACC.csv")

        val contents = ContentsDTO(fileName = "ACC.csv")
        val records = converter.convertFile(record, contents, accFile.inputStream())

        assertNotNull(record)
        assertEquals(6, records.size)
        assertEquals(true, records.last().endOfFileOffSet)
        assertEquals(1, records.filter { it.endOfFileOffSet }.size)
    }

    @Test
    @DisplayName("Should throw an exception if the headers do not match")
    fun testInvalidSchema() {
        val input =
                "TIMESTAMP    ,NOTTHIS               ,Y               ,Z\n" +
                "1553108459103,0.0082303769886 ,-0.0277035832405,0.0245985984802"
        val stream = input.toByteArray(Charsets.UTF_8)
                        .inputStream()

        val exception = assertThrows(ConversionFailedException::class.java) {
            converter.convertFile(record, contentsDTO, stream)
        }
        assertNotNull(exception)
    }


    @Test
    @DisplayName("Should throw Exception when invalid data type is sent in the file")
    fun testInvalidDataProcess() {
        val input =
                "TIMESTAMP    ,X               ,Y               ,Z\n" +
                        "1553108459103,corrupted ,-0.0277035832405,0.0245985984802"
        val stream = input.toByteArray(Charsets.UTF_8)
                .inputStream()

        val exception = assertThrows(ConversionFailedException::class.java) {
            converter.convertFile(record, contentsDTO, stream)
        }
        assertNotNull(exception)
    }
}
