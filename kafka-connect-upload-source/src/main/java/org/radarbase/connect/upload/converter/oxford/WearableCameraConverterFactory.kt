package org.radarbase.connect.upload.converter.oxford

import okhttp3.internal.closeQuietly
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.ZipFileProcessorFactory
import org.radarbase.connect.upload.io.FileUploader
import org.radarbase.connect.upload.io.SftpFileUploader
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL
import java.nio.file.Paths

class WearableCameraConverterFactory : ConverterFactory {
    override val sourceType: String = "oxford-wearable-camera"

    private val sftpUploader = ThreadLocal<FileUploader>()

    override fun fileProcessorFactories(settings: Map<String, String>, connectorConfig: SourceTypeDTO, logRepository: LogRepository): List<FileProcessorFactory> {
        val sourceConfig = checkNotNull(connectorConfig.configuration)
        val credentials = SftpFileUploader.SftpCredentials(
                host = sourceConfig.getValue("host"),
                port = sourceConfig["port"]?.toInt() ?: 22,
                username = sourceConfig.getValue("user"),
                password = sourceConfig["password"],
                privateKeyFile = sourceConfig["keyFile"],
                privateKeyPassphrase = sourceConfig["keyPassphrase"])

        val root = Paths.get(sourceConfig["root"] ?: ".")

        var urlString = sourceConfig["advertizedUrl"] ?: "sftp://${credentials.host}:${credentials.port}"
        if (!urlString.endsWith("/")) urlString += "/"
        val advertizedUrl = URI.create(urlString)

        logger.info("Advertised URL of sftp is set to $advertizedUrl")
        logger.info("Root folder for upload is $root")
        val processors = listOf(
                CameraDataFileProcessor(),
                CameraUploadProcessor(logRepository, { sftpUploader.get() }, root, advertizedUrl))
        return listOf(object : ZipFileProcessorFactory(processors, logRepository) {
            override fun beforeProcessing(contents: ContentsDTO) {
                sftpUploader.set(SftpFileUploader(credentials))
            }

            override fun afterProcessing(contents: ContentsDTO) {
                sftpUploader.get().closeQuietly()
                sftpUploader.remove()
            }

            override fun entryFilter(name: String) = ignoredFiles.none { name.contains(it) }
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WearableCameraConverterFactory::class.java)
        private val ignoredFiles = arrayOf(
                // downsized image directories
                "/256_192/", "/640_480/",
                // image binary data table
                "/.image_table")
    }
}
