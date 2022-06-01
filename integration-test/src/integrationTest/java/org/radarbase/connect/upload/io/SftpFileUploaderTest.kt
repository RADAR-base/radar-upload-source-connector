package org.radarbase.connect.upload.io

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SftpFileUploaderTest {
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
            "upload.source.file.uploader.type" to "sftp",
            "upload.source.file.uploader.target.endpoint" to "sftp://$HOST:$PORT/",
            "upload.source.file.uploader.target.root.directory" to "/upload",
            "upload.source.file.uploader.username" to USER,
            "upload.source.file.uploader.password" to PASSWORD,
        )

        converter = converterFactory.converter(
            settings,
            config,
            uploadBackendClient,
            logRepository,
        )
    }

    @Test
    @DisplayName("Should be able to upload files using sftp")
    fun testValidRawDataProcessing() {
        val imageRecords = TestBase.readOxfordZip(converter)
        imageRecords.forEach {
            assertNotNull(
                URL_REGEX.matchEntire(it.getUrl()),
                "URL ${it.getUrl()} does not match regex $URL_REGEX"
            )
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
    }
}
