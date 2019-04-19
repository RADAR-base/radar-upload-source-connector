package org.radarbase.connect.upload

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector
import org.radarbase.connect.upload.util.VersionUtil

class UploadSourceConnector : SourceConnector() {
    private lateinit var connectorConfig: UploadSourceConnectorConfig

    override fun taskConfigs(maxTasks: Int): List<Map<String, String>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun start(props: Map<String, String>?) {
        connectorConfig = UploadSourceConnectorConfig(props!!) // is it ok to assume this will not be null?

    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun version(): String = VersionUtil.getVersion()

    override fun taskClass(): Class<out Task> = UploadSourceTask::class.java

    override fun config(): ConfigDef = UploadSourceConnectorConfig.conf();

}
