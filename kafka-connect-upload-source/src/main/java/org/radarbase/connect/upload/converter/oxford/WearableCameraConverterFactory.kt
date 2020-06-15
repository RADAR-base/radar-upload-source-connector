package org.radarbase.connect.upload.converter.oxford

import okhttp3.internal.closeQuietly
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.ZipFileProcessorFactory
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

class WearableCameraConverterFactory : ConverterFactory {
    override val sourceType: String = "oxford-wearable-camera"

    private val localUploader = ThreadLocal<FileUploaderFactory.FileUploader>()

    override fun fileProcessorFactories(settings: Map<String, String>, connectorConfig: SourceTypeDTO, logRepository: LogRepository): List<FileProcessorFactory> {

        val uploaderSupplier = FileUploaderFactory(settings).fileUploader()
//        val uploadConfig = uploaderSupplier.config
//        val root: Path = Paths.get(uploaderSupplier.config.targetRoot)
//        val target = if (uploadConfig.targetEndpoint.endsWith("/")) uploadConfig.targetEndpoint else "${uploadConfig.targetEndpoint}/"
//        val advertisedUrl = URI(target)

        logger.info("Target endpoint is ${uploaderSupplier.advertisedTargetUri()} and Root folder for upload is ${uploaderSupplier.rootDirectory()}")
        val processors = listOf(
                CameraDataFileProcessor(),
                CameraUploadProcessor(logRepository, { localUploader.get() }))
        return listOf(object : ZipFileProcessorFactory(processors, logRepository) {
            override fun beforeProcessing(contents: ContentsDTO) {
                localUploader.set(uploaderSupplier)
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
