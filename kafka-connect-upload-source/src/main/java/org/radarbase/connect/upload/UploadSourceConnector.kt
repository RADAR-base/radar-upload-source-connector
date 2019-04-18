package org.radarbase.connect.upload

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector

class UploadSourceConnector : SourceConnector() {
    override fun taskConfigs(maxTasks: Int): List<Map<String, String>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun start(props: Map<String, String>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun version(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun taskClass(): Class<out Task> = UploadSourceTask::class.java

    override fun config(): ConfigDef {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
