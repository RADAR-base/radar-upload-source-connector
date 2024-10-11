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

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.radarbase.upload.doa.AbstractJpaPersistable
import java.time.LocalDateTime

@Entity
@Table(name = "record")
class Record : AbstractJpaPersistable<Long>() {
    @OneToOne(cascade = [CascadeType.ALL], mappedBy = "record")
    lateinit var metadata: RecordMetadata

    @Column(name = "project_id")
    lateinit var projectId: String

    @Column(name = "user_id")
    lateinit var userId: String

    @Column(name = "source_id")
    var sourceId: String? = null
    var time: LocalDateTime? = null

    @Column(name = "time_zone_offset")
    var timeZoneOffset: Int? = null

    @JoinColumn(name = "source_type")
    @ManyToOne
    lateinit var sourceType: SourceType

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "record", cascade = [CascadeType.ALL])
    var contents: MutableSet<RecordContent>? = null
}
