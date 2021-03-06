package org.radarbase.connect.upload.io

import com.nhaarman.mockitokotlin2.mock
import io.minio.MinioClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.*
import org.radarbase.connect.upload.converter.oxford.WearableCameraConverterFactory
import org.radarcns.connector.upload.oxford.OxfordCameraImage
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3FileUploaderTest {
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
                "upload.source.file.uploader.type" to "s3",
                "upload.source.file.uploader.target.endpoint" to ADVERTIZED_URL,
                "upload.source.file.uploader.target.root.directory" to ROOT,
                "upload.source.file.uploader.username" to USER,
                "upload.source.file.uploader.password" to PASSWORD
        )

        converter = converterFactory.converter(settings, config, uploadBackendClient, logRepository)
    }

    @Test
    @DisplayName("Should be able upload files to S3 bucket")
    fun testValidRawDataProcessing() {
        val zipName = "oxford-camera-sample.zip"

        val records = mutableListOf<TopicData>()
        val context = ConverterFactory.ContentsContext.create(
            record = record,
            contents = contentsDTO,
            logger = Mockito.mock(RecordLogger::class.java),
            avroData = RecordConverter.createAvroData(),
        )
        requireNotNull(javaClass.getResourceAsStream(zipName)).use { zipStream ->
            converter.convertFile(context, zipStream, records::add)
        }

        assertNotNull(record)
        assertEquals(10, records.size) // 5 images, 5x upload + 5x metadata
        val imageRecords = records.filter { it.value is OxfordCameraImage }
                .map { it.value as OxfordCameraImage }
        assertEquals(5, imageRecords.size)
        imageRecords.forEach {
            assertTrue(URL_REGEX.matchEntire(it.getUrl()) != null, "URL ${it.getUrl()} does not match regex $URL_REGEX")
        }

        val s3Client = MinioClient(ADVERTIZED_URL, USER, PASSWORD)
        val isExist: Boolean = s3Client.bucketExists(ROOT)
        assertTrue(isExist)

        s3Client.listObjects(ROOT).forEach {
            assertTrue(OBJECT_NAME_REGEX.matchEntire(it.get().objectName()) != null, "URL ${it.get().objectName()} does not match regex $OBJECT_NAME_REGEX")
        }
    }

    companion object {
        private const val ROOT = "target"
        private const val USER = "minioadmin"
        private const val PASSWORD = "minioadmin"
        private const val ADVERTIZED_URL = "http://localhost:9000/"
        private val URL_REGEX = Regex("http://localhost:9000/target/p/u/connect_upload_oxford_camera_image/1/2018-01-02/B0000000[0-9]_[^_]*_20180102_12[0-9]*E\\.jpg")
        private val OBJECT_NAME_REGEX = Regex("p/u/connect_upload_oxford_camera_image/1/2018-01-02/B0000000[0-9]_[^_]*_20180102_12[0-9]*E\\.jpg")
        private val logger = LoggerFactory.getLogger(S3FileUploaderTest::class.java)
    }
}
