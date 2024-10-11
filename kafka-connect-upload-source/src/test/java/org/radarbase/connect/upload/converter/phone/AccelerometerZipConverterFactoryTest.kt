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

package org.radarbase.connect.upload.converter.phone

import org.apache.kafka.connect.source.SourceRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.RecordDataDTO
import org.radarbase.connect.upload.api.RecordMetadataDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.RecordConverter
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.logging.ConverterLogRepository
import org.radarbase.connect.upload.logging.LogRepository
import org.radarbase.connect.upload.logging.RecordLogger
import java.io.File
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccelerometerZipConverterFactoryTest {
    private lateinit var context: ConverterFactory.ContentsContext
    private lateinit var converter: ConverterFactory.Converter

    private lateinit var logRepository: LogRepository

    private lateinit var uploadBackendClient: UploadBackendClient

    private val contentsDTO = ContentsDTO(
        contentType = "application/zip",
        fileName = "_ACC.zip",
        createdDate = Instant.now(),
        size = 1L,
    )

    private lateinit var record: RecordDTO

    @BeforeAll
    fun setUp() {
        uploadBackendClient = mock(UploadBackendClient::class.java)
        logRepository = ConverterLogRepository()
        val converterFactory = AccelerometerZipConverterFactory()
        val config = SourceTypeDTO(
            name = "phone-acceleration",
            configuration = emptyMap(),
            sourceIdRequired = false,
            timeRequired = false,
            topics = setOf("test_topic"),
            contentTypes = setOf(),
        )
        record = RecordDTO(
            id = 1L,
            metadata = RecordMetadataDTO(
                revision = 1,
                status = "PROCESSING",
            ),
            data = RecordDataDTO(
                projectId = "testProject",
                userId = "testUser",
                sourceId = "testSource",
                contents = setOf(
                    ContentsDTO(
                        fileName = "ACC.csv",
                    ),
                ),
            ),
            sourceType = "phone-acceleration",

        )
        context = ConverterFactory.ContentsContext.create(
            record = record,
            contents = contentsDTO,
            logger = mock(RecordLogger::class.java),
            avroData = RecordConverter.createAvroData(),
        )
        converter = converterFactory.converter(emptyMap(), config, uploadBackendClient, logRepository)
    }

    @Test
    @DisplayName("Should be able to convert a zip file to TopicRecords")
    fun testValidDataProcessing() {
        val accFile = File("src/test/resources/_ACC.zip")

        record.data?.contents?.firstOrNull()?.fileName = "_ACC.zip"

        val records = mutableListOf<SourceRecord>()
        converter.convertStream(
            record,
            openStream = { _, processStream ->
                processStream(accFile.inputStream())
            },
            records::add,
        )

        assertNotNull(records)
        assertEquals(6, records.size)
        assertEquals(true, records.last().sourceOffset()[END_OF_RECORD_KEY])
        assertEquals(1, records.filter { it.sourceOffset()[END_OF_RECORD_KEY] == true }.size)
    }

    @Test
    @DisplayName("Should throw an ConversionFailedException if the input is not a Zip file")
    fun testInValidDataProcessing() {
        val accFile = File("src/test/resources/ACC.csv")

        val exception = assertThrows(ConversionFailedException::class.java) {
            converter.convertFile(context, accFile.inputStream()) {}
        }
        assertNotNull(exception)
    }
}
