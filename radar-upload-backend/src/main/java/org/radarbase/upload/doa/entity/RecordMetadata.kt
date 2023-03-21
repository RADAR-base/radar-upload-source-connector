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

import java.time.Instant
import jakarta.persistence.*

@Entity
@Table(name = "record_metadata")
class RecordMetadata {
    @Id
    @Column(name = "record_id")
    var id: Long? = null

    @JoinColumn(name = "record_id")
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    lateinit var record: Record

    var revision: Int = 0

    @Enumerated(EnumType.STRING)
    var status: RecordStatus = RecordStatus.INCOMPLETE

    var message: String? = null

    @Column(name = "created_date")
    lateinit var createdDate: Instant

    @Column(name = "modified_date")
    lateinit var modifiedDate: Instant

    @Column(name = "committed_date")
    var committedDate: Instant? = null

    @OneToOne(mappedBy = "metadata", cascade = [CascadeType.REMOVE, CascadeType.DETACH, CascadeType.REFRESH])
    var logs: RecordLogs? = null

    @Column(name = "callback_url")
    var callbackUrl: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordMetadata

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
