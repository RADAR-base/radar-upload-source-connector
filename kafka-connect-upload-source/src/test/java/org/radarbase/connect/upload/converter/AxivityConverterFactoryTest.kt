package org.radarbase.connect.upload.converter

import org.radarbase.connect.upload.converter.axivity.AxivityConverterFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.radarbase.connect.upload.api.*
import java.io.File
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AxivityConverterFactoryTest {
    private lateinit var converter: ConverterFactory.Converter

    private lateinit var logRepository: LogRepository

    private lateinit var uploadBackendClient: UploadBackendClient

    private val contentsDTO = ContentsDTO(
            contentType = "application/zip",
            fileName = "sample.zip",
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
            sourceType = "axivity"

    )

    @BeforeAll
    fun setUp() {
        uploadBackendClient = Mockito.mock(UploadBackendClient::class.java)
        logRepository = ConverterLogRepository()
        val converterFactory = AxivityConverterFactory()
        val config = SourceTypeDTO(
                name = "axivity",
                configuration = emptyMap(),
                sourceIdRequired = false,
                timeRequired = false,
                topics = setOf("test_topic"),
                contentTypes = setOf()
        )
        converter = converterFactory.converter(emptyMap(), config, uploadBackendClient, logRepository)
    }

    @Test
    @DisplayName("Should be able to convert a zip file with sample data to TopicRecords")
    fun testValidRawDataProcessing() {
        val file = File("src/test/resources/sample.zip")

        val records = converter.convertFile(record, contentsDTO, file.inputStream(), Mockito.mock(RecordLogger::class.java))

        assertNotNull(records)
        assertTrue(records.size > 100)
        assertEquals(true, records.last().endOfFileOffSet)
        assertEquals(1, records.filter { it.endOfFileOffSet }.size)
    }

}
