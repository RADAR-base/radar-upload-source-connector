package org.radarbase.connect.upload.converter.gaitup

import okhttp3.internal.closeQuietly
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.radarbase.connect.upload.io.LocalFileUploader
import org.radarbase.connect.upload.io.SftpFileUploader
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

class Physilog5ConverterFactory : ConverterFactory {
    override val sourceType: String = "physilog5"

    private val localUploader = ThreadLocal<FileUploaderFactory.FileUploader>()

    override fun fileProcessorFactories(settings: Map<String, String>, connectorConfig: SourceTypeDTO, logRepository: LogRepository): List<FileProcessorFactory> {

        val uploaderSupplier = FileUploaderFactory(settings).fileUploader()

        logger.info("Physilog data will be uploaded using ${uploaderSupplier.type} to ${uploaderSupplier.advertisedTargetUri()} and ${uploaderSupplier.rootDirectory()}")
        return listOf( object :
                PhysilogUploadProcessorFactory(logRepository, { localUploader.get() }){
            override fun beforeProcessing(contents: ContentsDTO) {
                localUploader.set(uploaderSupplier)
            }

            override fun afterProcessing(contents: ContentsDTO) {
                localUploader.get().closeQuietly()
                localUploader.remove()
            }
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Physilog5ConverterFactory::class.java)
    }
}
