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

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.SEQUENCE
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.NaturalId
import org.hibernate.annotations.Parameter

@Entity
@Table(name = "source_type")
class SourceType {
    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "sequence-generator")
    @GenericGenerator(
        name = "sequence-generator",
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = [
            Parameter(name = "sequence_name", value = "hibernate_sequence"),
            Parameter(name = "initial_value", value = "1"),
            Parameter(name = "increment_size", value = "1"),
        ],
    )
    var id: Long? = null

    @Column
    @NaturalId
    lateinit var name: String

    @ElementCollection(fetch = FetchType.EAGER) // this is a collection of primitives
    @CollectionTable(name = "source_type_topics", joinColumns = [JoinColumn(name = "source_type_id")])
    @Column(name = "topic")
    var topics: Set<String> = HashSet()

    @ElementCollection(fetch = FetchType.EAGER) // this is a collection of primitives
    @Column(name = "content_type")
    @CollectionTable(name = "source_type_content_types", joinColumns = [JoinColumn(name = "source_type_id")])
    var contentTypes: Set<String> = HashSet()

    @Column(name = "time_required")
    var timeRequired = false

    @Column(name = "source_id_required")
    var sourceIdRequired = false

    @ElementCollection(fetch = FetchType.LAZY)
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @CollectionTable(name = "source_type_configuration", joinColumns = [JoinColumn(name = "source_type_id")])
    var configuration: Map<String, String> = HashMap()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SourceType

        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
