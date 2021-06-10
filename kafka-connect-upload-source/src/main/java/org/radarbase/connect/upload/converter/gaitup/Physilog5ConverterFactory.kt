package org.radarbase.connect.upload.converter.gaitup

import okhttp3.internal.closeQuietly
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.slf4j.LoggerFactory

class Physilog5ConverterFactory : ConverterFactory {
    override val sourceType: String = "physilog5"

    private val localUploader = ThreadLocal<FileUploaderFactory.FileUploader>()

    override fun fileProcessorFactories(
        settings: Map<String, String>,
        connectorConfig: SourceTypeDTO,
        logRepository: LogRepository,
    ): List<FileProcessorFactory> {
        val fileUploader = FileUploaderFactory(settings).fileUploader()

        logger.info(
            "Physilog data will be uploaded using {} to {} and {}",
            fileUploader.type,
            fileUploader.advertisedTargetUri,
            fileUploader.rootDirectory,
        )
        return listOf(object : PhysilogUploadProcessorFactory({ localUploader.get() }) {
            override fun beforeProcessing(context: ConverterFactory.ContentsContext) {
                localUploader.set(fileUploader.apply {
                    recordLogger = context.logger
                })
            }

            override fun afterProcessing(context: ConverterFactory.ContentsContext) {
                localUploader.get().apply {
                    recordLogger = null
                    closeQuietly()
                }
                localUploader.remove()
            }
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Physilog5ConverterFactory::class.java)
    }
}
