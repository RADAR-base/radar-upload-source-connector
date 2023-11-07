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

package org.radarbase.upload.doa

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter

@MappedSuperclass
abstract class AbstractJpaPersistable<T : java.io.Serializable> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence-generator")
    @GenericGenerator(
        name = "sequence-generator",
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = [
            Parameter(name = "sequence_name", value = "hibernate_sequence"),
            Parameter(name = "initial_value", value = "1"),
            Parameter(name = "increment_size", value = "1"),
        ],
    )
    var id: T? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other == null || javaClass != other.javaClass) return false

        other as AbstractJpaPersistable<*>

        return id != null && id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString() = "Entity<${javaClass.name}: $id>"

    companion object {
        inline fun <reified T : Any> T.equalTo(other: Any?, idGetter: T.() -> Any?, vararg getters: T.() -> Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as T

            val thisId = idGetter()
            val otherId = other.idGetter()

            return if (thisId != null && otherId != null) {
                thisId == otherId
            } else {
                getters.all { getter -> getter() == other.getter() }
            }
        }
    }
}
