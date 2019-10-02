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

package org.radarbase.upload.api

import org.radarbase.upload.doa.entity.SourceType

class SourceTypeMapperImpl : SourceTypeMapper {
    override fun fromSourceType(sourceType: SourceType) = SourceTypeDTO(
            name = sourceType.name,
            topics = sourceType.topics,
            contentTypes = sourceType.contentTypes,
            timeRequired = sourceType.timeRequired,
            sourceIdRequired = sourceType.sourceIdRequired,
            configuration = sourceType.configuration)

    override fun fromSourceTypes(sourceTypes: List<SourceType>) = SourceTypeContainerDTO(
            sourceTypes = sourceTypes.map(::fromSourceType))

    override fun toSourceType(sourceType: SourceTypeDTO) = SourceType().apply {
        name = sourceType.name
        topics = sourceType.topics?.toMutableSet() ?: mutableSetOf()
        contentTypes = sourceType.contentTypes?.toMutableSet() ?: mutableSetOf()
        sourceIdRequired = sourceType.sourceIdRequired ?: false
        timeRequired = sourceType.timeRequired ?: false
        configuration = sourceType.configuration?.toMutableMap() ?: mutableMapOf()
    }
}
