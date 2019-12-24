package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.converter.*
import org.radarbase.connect.upload.converter.axivity.CwaFileProcessorFactory.Companion.CWA_HEADER

class AxivityCsvProcessorFactory : OneToManyCsvLineProcessorFactory() {
    override val fileNameSuffix: String = ".cwa"
    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(fileNameSuffix, true)
    override val header: List<String> = CWA_HEADER
    override val recordMappers: List<LineToRecordMapper>
        get() = listOf(
            AxivityAccelerationCwaProcessor()
        )
}
