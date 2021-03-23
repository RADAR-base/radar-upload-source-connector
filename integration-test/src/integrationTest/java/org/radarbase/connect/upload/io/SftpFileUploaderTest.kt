package org.radarbase.connect.upload.io

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.ConverterLogRepository
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.oxford.WearableCameraConverterFactory
import org.radarcns.connector.upload.oxford.OxfordCameraImage
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SftpFileUploaderTest {
    private lateinit var converter: ConverterFactory.Converter

    private lateinit var logRepository: LogRepository

    private lateinit var uploadBackendClient: UploadBackendClient

    private val contentsDTO = ContentsDTO(
            contentType = "application/zip",
            fileName = "oxford-sample-data.zip",
            createdDate = Instant.now(),
            size = 1L
    )

    private val record = RecordDTO(
            id = 1L,
            metadata = RecordMetadataDTO(
                    revision = 1,
                    status = "PROCESSING"
            ),
            data = RecordDataDTO(
                    projectId = "p",
                    userId = "u",
                    sourceId = "s"
            ),
            sourceType = "oxford-wearable-camera"

    )

    @BeforeAll
    fun setUp() {
        uploadBackendClient = mock()
        logRepository = ConverterLogRepository()
        val converterFactory = WearableCameraConverterFactory()
        val config = SourceTypeDTO(
                name = "oxford-wearable-camera",
                configuration = emptyMap(),
                sourceIdRequired = false,
                timeRequired = false,
                topics = setOf(
                        "connect_upload_oxford_camera_image",
                        "connect_upload_oxford_camera_data"),
                contentTypes = setOf("application/zip")
        )

        val settings = mapOf(
                "upload.source.file.uploader.type" to "sftp",
                "upload.source.file.uploader.target.endpoint" to "sftp://$HOST:$PORT/",
                "upload.source.file.uploader.target.root.directory" to "/upload",
                "upload.source.file.uploader.username" to USER,
                "upload.source.file.uploader.password" to PASSWORD
        )

        converter = converterFactory.converter(settings, config, uploadBackendClient, logRepository)
    }

    @Test
    @DisplayName("Should be able to upload files using sftp")
    fun testValidRawDataProcessing() {
        val zipName = "oxford-camera-sample.zip"

        val records = converter.convertFile(
            record,
            contentsDTO,
            javaClass.getResourceAsStream(zipName),
            ConverterLogRepository.QueueRecordLogger(logger, 1L, LinkedList())
        ).toList()

        assertNotNull(record)
        assertEquals(10, records.size) // 5 images, 5x upload + 5x metadata
        assertEquals(true, records.last().endOfFileOffSet)
        assertEquals(1, records.filter { it.endOfFileOffSet }.size)
        val imageRecords = records.filter { it.value is OxfordCameraImage }
                .map { it.value as OxfordCameraImage }
        assertEquals(5, imageRecords.size)
        imageRecords.forEach {
            assertTrue(URL_REGEX.matchEntire(it.getUrl()) != null, "URL ${it.getUrl()} does not match regex $URL_REGEX")
        }

        val jsch = JSch()
        val session = jsch.getSession(USER, HOST, PORT)
        session.setPassword(PASSWORD)

        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        session.connect()
        (session.openChannel("sftp") as ChannelSftp).run {
            connect()
            // directory entries . and .. plus images.
            assertEquals(7, ls("upload/p/u/connect_upload_oxford_camera_image/1/2018-01-02").size)
            exit()
        }
        session.disconnect()
    }

    companion object {
        private const val HOST = "localhost"
        private const val PORT = 2222
        private const val USER = "connect"
        private const val PASSWORD = "pass"
        private val URL_REGEX = Regex("sftp://localhost:2222/upload/p/u/connect_upload_oxford_camera_image/1/2018-01-02/B0000000[0-9]_[^_]*_20180102_12[0-9]*E\\.jpg")
        private val logger = LoggerFactory.getLogger(SftpFileUploaderTest::class.java)
    }
}
