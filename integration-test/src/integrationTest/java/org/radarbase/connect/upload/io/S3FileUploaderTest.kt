package org.radarbase.connect.upload.io

import io.minio.BucketExistsArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.oxford.WearableCameraConverterFactory
import org.radarbase.connect.upload.logging.ConverterLogRepository
import org.radarbase.connect.upload.logging.LogRepository
import org.radarbase.connect.upload.util.TestBase

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3FileUploaderTest {
    private lateinit var converter: ConverterFactory.Converter

    private lateinit var logRepository: LogRepository

    private lateinit var uploadBackendClient: UploadBackendClient

    @BeforeAll
    fun setUp() {
        uploadBackendClient = mock()
        logRepository = ConverterLogRepository()
        val converterFactory = WearableCameraConverterFactory()
        val config = TestBase.oxfordZipSourceType

        val settings = mapOf(
            "upload.source.file.uploader.type" to "s3",
            "upload.source.file.uploader.target.endpoint" to ADVERTIZED_URL,
            "upload.source.file.uploader.target.root.directory" to ROOT,
            "upload.source.file.uploader.username" to USER,
            "upload.source.file.uploader.password" to PASSWORD,
        )

        converter = converterFactory.converter(settings, config, uploadBackendClient, logRepository)
    }

    @Test
    @DisplayName("Should be able upload files to S3 bucket")
    fun testValidRawDataProcessing() {
        val imageRecords = TestBase.readOxfordZip(converter)
        imageRecords.forEach {
            assertNotNull(
                URL_REGEX.matchEntire(it.getUrl()),
                "URL ${it.getUrl()} does not match regex $URL_REGEX",
            )
        }

        val s3Client = MinioClient.Builder()
            .endpoint(ADVERTIZED_URL)
            .credentials(USER, PASSWORD)
            .build()
        val isExist: Boolean = s3Client.bucketExists(
            BucketExistsArgs.builder()
                .bucket(ROOT)
                .build(),
        )
        assertTrue(isExist)

        s3Client.listObjects(ListObjectsArgs.builder().bucket(ROOT).recursive(true).build()).forEach {
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
    }
}
