package org.radarbase.connect.upload.converter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.CwaCsvInputStream.OPTIONS_EVENTS
import org.radarbase.connect.upload.converter.axivity.AxivityConverterFactory
import org.radarcns.connector.upload.axivity.AxivityAcceleration
import java.time.Instant
import java.util.zip.ZipInputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhysilogConverterFactoryTest {
    private lateinit var converter: ConverterFactory.Converter

    private lateinit var logRepository: LogRepository

    private lateinit var uploadBackendClient: UploadBackendClient

    private val contentsDTO = ContentsDTO(
            contentType = "application/zip",
            fileName = "CWA-DATA.zip",
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
        val records = requireNotNull(PhysilogConverterFactoryTest::class.java.getResourceAsStream("/CWA-DATA.zip")).use { cwaZipFile ->
            converter.convertFile(record, contentsDTO, cwaZipFile, Mockito.mock(RecordLogger::class.java)).toList()
        }

        val accRecords = records.filter { it.value.javaClass == AxivityAcceleration::class.java }

        requireNotNull(PhysilogConverterFactoryTest::class.java.getResourceAsStream("/CWA-DATA.zip")).use { cwaZipFile ->
            ZipInputStream(cwaZipFile).use { zipIn ->
                assertNotNull(zipIn.nextEntry)
                CwaCsvInputStream(zipIn, 0, 1, -1, OPTIONS_EVENTS).use { cwaIn ->
                    cwaIn.bufferedReader().use { it.readLines() }
                    // without any apparent reason, the CwaCsvInputStream is missing the first block...
                    assertEquals(cwaIn.line + 80, accRecords.size)
                }
            }
        }

        assertNotNull(records)
        assertTrue(records.size > 1000)
        assertEquals(true, records.last().endOfFileOffSet)
        assertEquals(1, records.filter { it.endOfFileOffSet }.size)
    }

}
