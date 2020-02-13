package org.radarbase.connect.upload.converter.oxford

import okhttp3.internal.closeQuietly
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.ZipFileProcessorFactory
import org.radarbase.connect.upload.io.FileUploader
import org.radarbase.connect.upload.io.LocalFileUploader
import org.radarbase.connect.upload.io.SftpFileUploader
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

class WearableCameraConverterFactory : ConverterFactory {
    override val sourceType: String = "oxford-wearable-camera"

    private val localUploader = ThreadLocal<FileUploader>()

    override fun fileProcessorFactories(settings: Map<String, String>, connectorConfig: SourceTypeDTO, logRepository: LogRepository): List<FileProcessorFactory> {
        val sourceConfig = checkNotNull(connectorConfig.configuration)

        val advertizedUrl: URI
        val uploaderSupplier: () -> FileUploader
        val root: Path

        if ("host" in sourceConfig) {
            val credentials = SftpFileUploader.SftpCredentials(
                    host = sourceConfig.getValue("host"),
                    port = sourceConfig["port"]?.toInt() ?: 22,
                    username = sourceConfig.getValue("user"),
                    password = sourceConfig["password"],
                    privateKeyFile = sourceConfig["keyFile"],
                    privateKeyPassphrase = sourceConfig["keyPassphrase"])

            var urlString = sourceConfig["advertizedUrl"] ?: "sftp://${credentials.host}:${credentials.port}"
            if (!urlString.endsWith("/")) urlString += "/"
            advertizedUrl = URI.create(urlString)
            uploaderSupplier = { SftpFileUploader(credentials) }
            root = Paths.get(sourceConfig["root"] ?: ".")
            logger.info("Advertised URL of sftp is set to $advertizedUrl")
            logger.info("Storing wearable camera images to SFTP server {}", credentials.host)
        } else {
            advertizedUrl = URI.create(sourceConfig["advertizedUrl"] ?: "file://")
            uploaderSupplier = { LocalFileUploader() }
            root = Paths.get(sourceConfig["root"] ?: ".").toAbsolutePath()
            logger.info("Storing wearable camera images to the local file system")
        }

        logger.info("Root folder for upload is $root")
        val processors = listOf(
                CameraDataFileProcessor(),
                CameraUploadProcessor(logRepository, { localUploader.get() }, root, advertizedUrl))
        return listOf(object : ZipFileProcessorFactory(processors, logRepository) {
            override fun beforeProcessing(contents: ContentsDTO) {
                localUploader.set(uploaderSupplier())
            }

            override fun afterProcessing(contents: ContentsDTO) {
                localUploader.get().closeQuietly()
                localUploader.remove()
            }

            override fun entryFilter(name: String) = ignoredFiles.none { name.contains(it, true) }
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WearableCameraConverterFactory::class.java)
        private val ignoredFiles = arrayOf(
                // downsized image directories
                "/256_192/", "/640_480/",
                // image binary data table
                "/.image_table",
                // ACTIVITY.CSV no knowledge about the content.
                "ACTIVITY.CSV")
    }
}
