package org.radarbase.connect.upload.altoida

import io.confluent.connect.avro.AvroData
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.RecordConverter
import org.radarbase.connect.upload.converter.TopicData
import java.io.InputStream

class AltoidaCsvExportConverter(sourceType : String = "altoida-csv-export", avroData: AvroData = AvroData(20)):
        RecordConverter(sourceType, avroData) {


    override fun processData(inputStream: InputStream, record: RecordDTO, timeReceived: Double): Sequence<TopicData> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPartition(): MutableMap<String, Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}
