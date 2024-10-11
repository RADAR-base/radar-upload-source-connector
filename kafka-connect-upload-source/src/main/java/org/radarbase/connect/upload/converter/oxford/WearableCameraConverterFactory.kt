package org.radarbase.connect.upload.converter.oxford

import okhttp3.internal.closeQuietly
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.archive.ArchiveProcessorFactory
import org.radarbase.connect.upload.converter.archive.ZipInputStreamIterator.Companion.zipIteratorFactory
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.radarbase.connect.upload.logging.LogRepository
import org.slf4j.LoggerFactory

class WearableCameraConverterFactory : ConverterFactory {
    override val sourceType: String = "oxford-wearable-camera"

    private val localThreadUploader = ThreadLocal<FileUploaderFactory.FileUploader>()

    override fun fileProcessorFactories(
        settings: Map<String, String>,
        connectorConfig: SourceTypeDTO,
        logRepository: LogRepository,
    ): List<FileProcessorFactory> {
        val uploader = FileUploaderFactory(settings).fileUploader()

        logger.info(
            "Target endpoint is {} and Root folder for upload is {}",
            uploader.advertisedTargetUri,
            uploader.rootDirectory,
        )
        val processors = listOf(
            CameraDataFileProcessor(),
            CameraUploadProcessor { localThreadUploader.get() },
        )
        return listOf(
            object : ArchiveProcessorFactory(
                sourceType,
                entryProcessors = processors,
                extension = ".zip",
                archiveIteratorFactory = zipIteratorFactory,
            ) {
                override fun beforeProcessing(contents: ConverterFactory.ContentsContext) {
                    localThreadUploader.set(
                        uploader.apply {
                            recordLogger = contents.logger
                        },
                    )
                }

                override fun afterProcessing(contents: ConverterFactory.ContentsContext) {
                    localThreadUploader.get().run {
                        recordLogger = null
                        closeQuietly()
                    }
                    localThreadUploader.remove()
                }

                override fun entryFilter(name: String) =
                    ignoredFiles.none { name.contains(it, true) }
            },
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WearableCameraConverterFactory::class.java)
        private val ignoredFiles = arrayOf(
            // downsized image directories
            "/256_192/",
            "/640_480/",
            // image binary data table
            "/.image_table",
            // ACTIVITY.CSV no knowledge about the content.
            "ACTIVITY.CSV",
        )
    }
}
