package org.radarbase.connect.upload.converter.oxford

import okhttp3.internal.closeQuietly
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.logging.LogRepository
import org.radarbase.connect.upload.converter.zip.ZipFileProcessorFactory
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.slf4j.LoggerFactory

class WearableCameraConverterFactory : ConverterFactory {
    override val sourceType: String = "oxford-wearable-camera"

    private val localUploader = ThreadLocal<FileUploaderFactory.FileUploader>()

    override fun fileProcessorFactories(
        settings: Map<String, String>,
        connectorConfig: SourceTypeDTO,
        logRepository: LogRepository,
    ): List<FileProcessorFactory> {
        val uploaderSupplier = FileUploaderFactory(settings).fileUploader()

        logger.info(
            "Target endpoint is {} and Root folder for upload is {}",
            uploaderSupplier.advertisedTargetUri,
            uploaderSupplier.rootDirectory,
        )
        val processors = listOf(
            CameraDataFileProcessor(),
            CameraUploadProcessor { localUploader.get() },
        )
        return listOf(object : ZipFileProcessorFactory(sourceType, zipEntryProcessors = processors) {
            override fun beforeProcessing(contents: ConverterFactory.ContentsContext) {
                localUploader.set(uploaderSupplier.apply {
                    recordLogger = contents.logger
                })
            }

            override fun afterProcessing(contents: ConverterFactory.ContentsContext) {
                localUploader.get().run {
                    recordLogger = null
                    closeQuietly()
                }
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
