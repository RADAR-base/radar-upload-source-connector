package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.altoida.AltoidaObjects

class AltoidaObjectsConverter(override val sourceType: String = "objects", val topic: String = "connect_upload_altoida_objects")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "OBJECT", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val objects = AltoidaObjects(
                time,
                timeReceived,
                lineValues["OBJECT"]?.toString(),
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat(),
                )

        return TopicData(false, topic, objects)
    }
}
