package org.radarbase.connect.upload.converter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.altoida.AltoidaConverterFactory
import java.io.File
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AltoidaCoverterFactoryTest {
    private lateinit var converter: ConverterFactory.Converter

    private lateinit var logRepository: LogRepository

    private lateinit var uploadBackendClient: UploadBackendClient

    private val record = RecordDTO(
            id = 1L,
            metadata = RecordMetadataDTO(
                    revision = 1,
                    status = "PROCESSING"
            ),
            data = null,
            sourceType = "altoida"

    )

    @BeforeAll
    fun setUp() {
        uploadBackendClient = Mockito.mock(UploadBackendClient::class.java)
        logRepository = ConverterLogRepository()
        val converterFactory = AltoidaConverterFactory()
        val config = SourceTypeDTO(
                name = "altoida",
                configuration = emptyMap(),
                sourceIdRequired = false,
                timeRequired = false,
                topics = setOf("test_topic"),
                contentTypes = setOf()
        )
        converter = converterFactory.converter(emptyMap(), config, uploadBackendClient, logRepository)
    }

    @Test
    @DisplayName("Should be able to convert a iOS zip file to TopicRecords")
    fun testValidRawIosDataProcessing() {
        val file = File("src/test/resources/TEST_ZIP.zip")

        val records = converter.convertFile(record, ContentsDTO(
                contentType = "application/zip",
                fileName = "TEST_ZIP.zip",
                createdDate = Instant.now(),
                size = 1L
        ), file.inputStream(), Mockito.mock(RecordLogger::class.java))

        assertNotNull(records)
        assertTrue(records.size > 1000)
        assertEquals(true, records.last().endOfFileOffSet)
        assertEquals(1, records.filter { it.endOfFileOffSet }.size)
    }


    @Test
    @DisplayName("Should be able to convert an Android zip file to TopicRecords")
    fun testValidRawAndroidDataProcessing() {
        val file = File("src/test/resources/ALTOIDA_ANDROID.zip")

        val records = converter.convertFile(record, ContentsDTO(
                contentType = "application/zip",
                fileName = "ALTOIDA_ANDROID.zip",
                createdDate = Instant.now(),
                size = 1L
        ), file.inputStream(), Mockito.mock(RecordLogger::class.java))

        assertNotNull(records)
        assertTrue(records.size > 1000)
        assertEquals(true, records.last().endOfFileOffSet)
        assertEquals(1, records.filter { it.endOfFileOffSet }.size)
    }

    @Test
    @DisplayName("Should be able to convert export.csv to TopicRecords")
    fun testValidExportCsvProcessing() {
        val file = File("src/test/resources/export.csv")

        val contentsDTO = ContentsDTO(
                contentType = "text/csv",
                fileName = "export.csv",
                createdDate = Instant.now(),
                size = 1L
        )
        val records = converter.convertFile(record, contentsDTO, file.inputStream(), Mockito.mock(RecordLogger::class.java))

        assertNotNull(records)
        assertEquals(records.size, 4)
        assertEquals(true, records.last().endOfFileOffSet)
        assertEquals(1, records.filter { it.endOfFileOffSet }.size)

        val expectedTopics = listOf(
                "connect_upload_altoida_bit_metrics",
                "connect_upload_altoida_dot_metrics",
                "connect_upload_altoida_summary",
                "connect_upload_altoida_domain_result"
        )
        assertTrue(records.map { it.topic }.containsAll(expectedTopics))
    }

}
