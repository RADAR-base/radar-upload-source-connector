package org.radarbase.upload.doa.entity

import java.sql.Blob
import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "record_content")
class RecordContent {
    @Id
    @Column(name = "record_id")
    var id: Long? = null

    @Column(name = "file_name")
    lateinit var fileName: String

    @Column(name = "content_type")
    lateinit var contentType: String

    @Column(name = "created_date")
    lateinit var createdDate: Instant

    @Column
    var size: Long = 0

    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.REFRESH, CascadeType.MERGE, CascadeType.PERSIST])
    @JoinColumn(name = "record_id")
    @MapsId
    lateinit var record: Record

    @Basic(fetch = FetchType.LAZY)
    lateinit var content: Blob
}
