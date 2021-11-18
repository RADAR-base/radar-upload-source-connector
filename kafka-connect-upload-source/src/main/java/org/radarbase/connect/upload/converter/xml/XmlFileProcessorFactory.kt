/*
 *  Copyright 2019 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.connect.upload.converter.csv

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.xml.StatelessXmlLineProcessor

open class XmlFileProcessorFactory(
    open val xmlProcessorFactories: List<StatelessXmlLineProcessor>,
) : FileProcessorFactory {
    override fun matches(contents: ContentsDTO) = xmlProcessorFactories.any { it.matches(contents) }

    override fun createProcessor(record: RecordDTO) = XmlProcessor(record, xmlProcessorFactories)
}
