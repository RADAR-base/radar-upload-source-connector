package org.radarbase.connect.upload.converter

import java.io.InputStream

/** Processor to process a single file in a record. */
interface FilePreProcessor {
    /**
     * Preprocess the [inputStream] of [context] to ensure that the file can be processed by
     * other processors.
     */
    fun preProcessFile(
        context: ConverterFactory.ContentsContext,
        inputStream: InputStream,
    ): InputStream
}
