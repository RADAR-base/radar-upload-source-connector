package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.converter.*

class AxivityCsvProcessor(override val header: List<String>) : OneToManyCsvProcessor() {
    override val fileNameSuffix: String = ".cwa"
    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(fileNameSuffix, true)
    override val recordMappers: List<LineToRecordMapper>
        get() = listOf(
            AxivityAccelerationProcessor()
        )
}
