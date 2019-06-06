package org.radarbase.upload.doa.entity

import org.radarbase.upload.doa.AbstractJpaPersistable
import java.time.LocalDateTime
import javax.persistence.*

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
