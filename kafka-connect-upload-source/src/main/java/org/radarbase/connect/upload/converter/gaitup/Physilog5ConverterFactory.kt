package org.radarbase.connect.upload.converter.gaitup

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

class Physilog5ConverterFactory : ConverterFactory {
    override val sourceType: String = "physilog5"

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
            logger.info("Advertised URL of sftp is set to $advertizedUrl and the root for upload is $root")
            logger.info("Storing physilog4 binary data to SFTP server {}", credentials.host)
        } else {
            advertizedUrl = URI.create(sourceConfig["advertizedUrl"] ?: "file://")
            uploaderSupplier = { LocalFileUploader() }
            root = Paths.get(sourceConfig["root"] ?: ".").toAbsolutePath()
            logger.info("Storing physilog4 binary data to the local file system")
            logger.info("Advertised URL of local file upload is set to $advertizedUrl and the root for upload is $root")
        }


        return listOf( object :
                PhysilogUploadProcessorFactory(logRepository, { localUploader.get() }, root, advertizedUrl){
            override fun beforeProcessing(contents: ContentsDTO) {
                localUploader.set(uploaderSupplier())
            }

            override fun afterProcessing(contents: ContentsDTO) {
                localUploader.get().closeQuietly()
                localUploader.remove()
            }
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Physilog5ConverterFactory::class.java)
        private val ignoredFiles = arrayOf(
                // downsized image directories
                "/256_192/", "/640_480/",
                // image binary data table
                "/.image_table",
                // ACTIVITY.CSV no knowledge about the content.
                "ACTIVITY.CSV")
    }
}
