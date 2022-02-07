/*
 *  Copyright 2019 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.connect.upload.logging

import org.slf4j.Logger

interface LogRepository {
    /**
     * Create a logger for logging about a single record.
     *
     * @param logger system logger to write log messages with.
     * @param recordId record that the logging should refer to.
     */
    fun createLogger(logger: Logger, recordId: Long): RecordLogger

    /**
     * Create a logger for logging about a single record.
     *
     * @param clazz class that the system logger should refer to
     * @param recordId record that the logging should refer to
     */
    fun createLogger(clazz: Class<*>, recordId: Long): RecordLogger

    /** Record IDs that have logs set. */
    val recordIds: Set<Long>

    /**
     * Extract a log from a single record.
     *
     * @param recordId record to extract log from
     * @param reset whether to remove the log after extracting it
     */
    fun extract(recordId: Long, reset: Boolean = false): Log?
}

