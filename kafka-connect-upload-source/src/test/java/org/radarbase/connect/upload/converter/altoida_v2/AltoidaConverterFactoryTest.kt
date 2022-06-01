package org.radarbase.connect.upload.converter.altoida_v2

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.radarbase.connect.upload.api.*
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
                status = "PROCESSING"
            ),
            data = RecordDataDTO(
                projectId = "testProject",
                userId = "testUser",
                sourceId = "testSource",
            ),
            sourceType = "altoida_v2"

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
    @DisplayName("Should be able to convert a iOS zip file to TopicRecords")
    fun testValidRawIosDataProcessing() {
        val file = File("src/test/resources/v2-ALTOIDA.zip")

        val context = ConverterFactory.ContentsContext.create(
            record = record,
            contents = ContentsDTO(
                contentType = "application/zip",
                fileName = "TEST_ZIP.zip",
                createdDate = Instant.now(),
                size = 1L
            ),
            logger = Mockito.mock(RecordLogger::class.java),
            avroData = RecordConverter.createAvroData(),
        )

        val records = mutableListOf<TopicData>()
        converter.convertFile(context, file.inputStream(), records::add)

        assertNotNull(records)
        assertThat(records.count(), greaterThan(1000))

        val expectedTopics = listOf(
            "connect_upload_altoida_touch",
            "connect_upload_altoida_acceleration",
            "connect_upload_altoida_path",
            "connect_upload_altoida_attitude",
            "connect_upload_altoida_gravity",
            "connect_upload_altoida_magnetic_field",
            // XML file topics
            "connect_upload_altoida_ar_assessment",
            "connect_upload_altoida_assessment",
            "connect_upload_altoida_xml_metadata",
            "connect_upload_altoida_screen_elements",
            "connect_upload_altoida_test_event"
        )
        assertTrue(records.map { it.topic }.containsAll(expectedTopics))
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
                size = 1L
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
                "connect_upload_altoida_domain_result"
        )
        assertTrue(records.map { it.topic }.containsAll(expectedTopics))
    }

}
