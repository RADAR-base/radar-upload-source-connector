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
import java.sql.Blob
import java.time.Instant
import java.util.*
import jakarta.persistence.*

@Entity
@Table(name = "record_content")
class RecordContent : AbstractJpaPersistable<Long>() {
    @Column(name = "file_name")
    lateinit var fileName: String

    @Column(name = "content_type")
    lateinit var contentType: String

    @Column(name = "created_date")
    lateinit var createdDate: Instant

    @Column
    var size: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    lateinit var record: Record

    @Lob
    @Basic(fetch = FetchType.LAZY)
    lateinit var content: Blob

    override fun equals(other: Any?): Boolean = equalTo(
        other,
        idGetter = { id },
        { record },
        { fileName },
        { createdDate },
        { size },
    )
    override fun hashCode(): Int = Objects.hash(record, fileName)
}
