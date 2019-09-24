/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

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
