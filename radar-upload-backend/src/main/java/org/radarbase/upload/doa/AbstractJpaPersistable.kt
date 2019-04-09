package org.radarbase.upload.doa

import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AbstractJpaPersistable<T : java.io.Serializable> {
    @Id
    @GeneratedValue
    var id: T? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other == null || javaClass != other.javaClass) return false

        other as AbstractJpaPersistable<*>

        return id != null && id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString() = "Entity of type ${this.javaClass.name} with id: $id"
}
