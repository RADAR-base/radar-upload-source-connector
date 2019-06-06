package org.radarbase.connect.upload

import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector
import org.radarbase.connect.upload.util.VersionUtil
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.HashMap

class UploadSourceConnector : SourceConnector() {
    private lateinit var connectorConfig: UploadSourceConnectorConfig

    override fun taskConfigs(maxTasks: Int): List<Map<String, String>> =
            Collections.nCopies(maxTasks, HashMap(connectorConfig.originalsStrings()))

    override fun start(props: Map<String, String>?) {
        connectorConfig = UploadSourceConnectorConfig(props!!)
    }

    override fun stop() {
        logger.debug("Stopping source task")
    }

    override fun version(): String = VersionUtil.getVersion()

    override fun taskClass(): Class<out Task> = UploadSourceTask::class.java

    override fun config(): ConfigDef = UploadSourceConnectorConfig.conf()

    companion object {
        private val logger = LoggerFactory.getLogger(UploadSourceConnector::class.java)
    }

}
