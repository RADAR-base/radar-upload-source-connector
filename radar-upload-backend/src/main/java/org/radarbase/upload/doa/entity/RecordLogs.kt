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

package org.radarbase.upload.doa.entity

import org.radarbase.upload.doa.AbstractJpaPersistable
import java.sql.Clob
import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "record_logs")
class RecordLogs : AbstractJpaPersistable<Long>(){


    @Column(name = "modified_date")
    lateinit var modifiedDate: Instant

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    lateinit var metadata: RecordMetadata

    var size: Long = 0

    @Enumerated(EnumType.STRING)
    var level: LogLevel? = LogLevel.INFO

    lateinit var logs: Clob

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordLogs

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}


enum class LogLevel {
    INFO,
    DEBUG,
    WARN,
    ERROR
}
