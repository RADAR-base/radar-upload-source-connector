package org.radarbase.connect.upload.converter

import java.io.InputStream

/** Processor to process a single file in a record. */
interface FileProcessor {
    /**
     * Process record contents from [context] using [inputStream] and sends the result to [produce].
     */
    fun processData(
        context: ConverterFactory.ContentsContext,
        inputStream: InputStream,
        produce: (TopicData) -> Unit,
    )
}
