package org.radarbase.connect.upload.converter.altoidav2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.RecordDataDTO
import org.radarbase.connect.upload.api.RecordMetadataDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.RecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.logging.ConverterLogRepository
import org.radarbase.connect.upload.logging.LogRepository
import org.radarbase.connect.upload.logging.RecordLogger
import java.io.File
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AltoidaConverterFactoryTest {
    private lateinit var converter: ConverterFactory.Converter

    private lateinit var logRepository: LogRepository

    private lateinit var uploadBackendClient: UploadBackendClient

    private val record = RecordDTO(
        id = 1L,
        metadata = RecordMetadataDTO(
            revision = 1,
            status = "PROCESSING",
        ),
        data = RecordDataDTO(
            projectId = "testProject",
            userId = "testUser",
            sourceId = "testSource",
        ),
        sourceType = "altoida_v2",

    )

    @BeforeAll
    fun setUp() {
        uploadBackendClient = Mockito.mock(UploadBackendClient::class.java)
        logRepository = ConverterLogRepository()
        val converterFactory = AltoidaConverterFactory()
        val config = SourceTypeDTO(
            name = "altoida_v2",
            configuration = emptyMap(),
            sourceIdRequired = false,
            timeRequired = false,
            topics = setOf("test_topic"),
            contentTypes = setOf(),
        )
        converter = converterFactory.converter(emptyMap(), config, uploadBackendClient, logRepository)
    }

    @Test
    @DisplayName("Should be able to convert export.csv to TopicRecords")
    fun testValidExportCsvProcessing() {
        val file = File("src/test/resources/v2-export.csv")

        val context = ConverterFactory.ContentsContext.create(
            record = record,
            contents = ContentsDTO(
                contentType = "text/csv",
                fileName = "export.csv",
                createdDate = Instant.now(),
                size = 1L,
            ),
            logger = Mockito.mock(RecordLogger::class.java),
            avroData = RecordConverter.createAvroData(),
        )

        val records = mutableListOf<TopicData>()
        converter.convertFile(context, file.inputStream(), records::add)

        assertNotNull(records)
        assertEquals(records.count(), 2)

        val expectedTopics = listOf(
            "connect_upload_altoida_summary",
            "connect_upload_altoida_domain_result",
        )
        assertTrue(records.map { it.topic }.containsAll(expectedTopics))
    }
}
