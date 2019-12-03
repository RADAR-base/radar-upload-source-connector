package org.radarbase.connect.upload.converter.oxford

import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.ZipFileProcessorFactory
import org.radarbase.connect.upload.io.SftpFileUploader
import java.net.URL
import java.nio.file.Paths

class WearableCameraConverterFactory : ConverterFactory {
    override val sourceType: String = "oxford-wearable-camera"

    override fun fileProcessorFactories(settings: Map<String, String>, connectorConfig: SourceTypeDTO, logRepository: LogRepository): List<FileProcessorFactory> {
        val sourceConfig = checkNotNull(connectorConfig.configuration)
        val credentials = SftpFileUploader.SftpCredentials(
                host = sourceConfig.getValue("host"),
                port = sourceConfig["port"]?.toInt() ?: 22,
                username = sourceConfig.getValue("user"),
                password = sourceConfig["password"],
                privateKeyFile = sourceConfig["keyFile"],
                privateKeyPassphrase = sourceConfig["keyPassphrase"])

        val root = Paths.get(sourceConfig.getValue("root"))

        val advertizedUrl = URL(sourceConfig["advertizedUrl"] ?: "sftp://${credentials.host}:${credentials.port}")

        val processors = listOf(CameraDataFileProcessor(), CameraUploadProcessor(credentials, root, advertizedUrl))
        return listOf(ZipFileProcessorFactory(processors, logRepository) { name ->
            !name.contains("/256_192/") && !name.contains("/640_480/")})
    }
}
