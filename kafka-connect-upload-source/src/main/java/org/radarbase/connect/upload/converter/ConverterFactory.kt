/*
 *  Copyright 2019 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.connect.upload.converter

import io.confluent.connect.avro.AvroData
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.errors.ConnectException
import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.upload.api.*
import org.radarcns.kafka.ObservationKey
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path

/**
 * Converter for each source-type
 */
interface ConverterFactory {
    val sourceType: String

    fun fileProcessorFactories(
        settings: Map<String, String>,
        connectorConfig: SourceTypeDTO,
        logRepository: LogRepository,
    ): List<FileProcessorFactory>

    fun converter(
        settings: Map<String, String>,
        connectorConfig: SourceTypeDTO,
        client: UploadBackendClient,
        logRepository: LogRepository,
    ): Converter {
        val processors = fileProcessorFactories(settings, connectorConfig, logRepository)
        return RecordConverter(sourceType, processors, client, logRepository)
    }

    /**
     * Converter for each source-type
     */
    interface Converter : Closeable {
        val sourceType: String

        // convert and add logs return result
        fun convert(
            record: RecordDTO,
            produce: (SourceRecord) -> Unit,
        )

        fun convertFile(
            context: ContentsContext,
            inputStream: InputStream,
            produce: (TopicData) -> Unit,
        )

        fun convertStream(
            record: RecordDTO,
            useStream: (ContentsContext, (InputStream) -> Unit) -> Unit,
            produce: (SourceRecord) -> Unit
        )

        fun getPartition(): MutableMap<String, Any>

        companion object {
            const val END_OF_RECORD_KEY = "endOfRecord"
            const val RECORD_ID_KEY = "recordId"
            const val REVISION_KEY = "versionId"
        }
    }

    class ContentsContext(
        val id: Long,
        val record: RecordDTO,
        val contents: ContentsDTO,
        val metadata: RecordMetadataDTO,
        val data: RecordDataDTO,
        val logger: RecordLogger,
        val avroData: AvroData,
    ) {
        val fileName: String get() = contents.fileName
        val key: SchemaAndValue by lazy {
            avroData.toConnectData(
                ObservationKey.getClassSchema(),
                ObservationKey(
                    data.projectId,
                    data.userId,
                    data.sourceId,
                )
            )
        }

        companion object {
            fun create(
                record: RecordDTO,
                contents: ContentsDTO,
                logger: RecordLogger,
                avroData: AvroData,
            ): ContentsContext {
                val id = checkNotNull(record.id)
                val data = checkNotNull(record.data) { "Record data cannot be null" }
                val metadata = checkNotNull(record.metadata) { "Record meta-data cannot be null" }

                return ContentsContext(
                    id = id,
                    record = record,
                    data = data,
                    contents = contents,
                    metadata = metadata,
                    logger = logger,
                    avroData = avroData,
                )
            }

            fun createAll(
                record: RecordDTO,
                logRepository: LogRepository,
                avroData: AvroData,
            ): List<ContentsContext> {
                val id = checkNotNull(record.id)
                val data = checkNotNull(record.data) { "Record data cannot be null" }
                val metadata = checkNotNull(record.metadata) { "Record meta-data cannot be null" }
                val logger = logRepository.createLogger(logger, id)

                return checkNotNull(data.contents) { "Record data has empty content" }
                    .map { contents ->
                        ContentsContext(
                            id = id,
                            record = record,
                            data = data,
                            contents = contents,
                            metadata = metadata,
                            logger = logger,
                            avroData = avroData,
                        )
                    }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConverterFactory::class.java)

        fun fromClassName(
            name: String): ConverterFactory {
            return try {
                Class.forName(name)
                    .getDeclaredConstructor()
                    .newInstance() as ConverterFactory
            } catch (exe: ClassCastException) {
                throw ConnectException("Class $name for converter is not a Converter implementation", exe)
            } catch (exe: ClassNotFoundException) {
                throw ConnectException("Converter class $name not found in class path", exe)
            } catch (exe: ReflectiveOperationException) {
                throw ConnectException("Converter class $name could not be instantiated", exe)
            } catch (exe: Exception) {
                throw ConnectException("Cannot successfully initialize converter $name", exe)
            }
        }

        fun createConverter(factoryClassName: String,
            settings: Map<String, String>,
            client: UploadBackendClient,
            logRepository: LogRepository
        ): Converter {
            val converterFactory = fromClassName(factoryClassName)
            val config = client.requestConnectorConfig(converterFactory.sourceType)
            return converterFactory.converter(settings, config, client, logRepository)
        }
    }
}
