package org.radarbase.connect.upload.converter

import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarcns.passive.phone.PhoneAcceleration

class AccelerometerCsvRecordConverter(override val sourceType: String = "phone-acceleration", val topic: String = "android_phone_acceleration")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z") == (csvHeader)


    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val acceleration = PhoneAcceleration(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()
        )

        return TopicData(false, topic, acceleration)
    }

    override fun commitLogs(record: RecordDTO, client: UploadBackendClient) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
