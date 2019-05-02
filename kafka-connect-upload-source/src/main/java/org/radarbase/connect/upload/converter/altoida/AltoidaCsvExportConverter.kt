package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData

class AltoidaCsvExportConverter(sourceType : String = "altoida-csv-export"):
        CsvRecordConverter(sourceType) {
    override fun commitLogs(record: RecordDTO, client: UploadBackendClient) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun validateHeaderSchema(csvHeader: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun convertLineToRecord(lineValues: Map<String, String>): TopicData {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}
