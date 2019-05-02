package org.radarbase.upload.doa.entity

import org.hibernate.annotations.NaturalId
import javax.persistence.*

@Entity
@Table(name = "source_type")
class SourceType {
    @Id
    @GeneratedValue
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
