package org.radarbase.upload.doa.entity

import java.sql.Clob
import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "record_logs")
class RecordLogs {
    @Id
    @Column(name = "record_id")
    var id: Long? = null

    @Column(name = "modified_date")
    lateinit var modifiedDate: Instant

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    @MapsId
    lateinit var metadata: RecordMetadata

    var size: Long = 0
    lateinit var logs: Clob

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordLogs

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
