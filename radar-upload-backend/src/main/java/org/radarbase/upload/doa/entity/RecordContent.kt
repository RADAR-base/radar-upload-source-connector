package org.radarbase.upload.doa.entity

import org.radarbase.upload.doa.AbstractJpaPersistable
import java.sql.Blob
import java.time.Instant
import javax.persistence.*

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

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.REFRESH, CascadeType.MERGE, CascadeType.PERSIST])
    @JoinColumn(name = "record_id")
    lateinit var record: Record

    @Basic(fetch = FetchType.LAZY)
    lateinit var content: Blob
}
