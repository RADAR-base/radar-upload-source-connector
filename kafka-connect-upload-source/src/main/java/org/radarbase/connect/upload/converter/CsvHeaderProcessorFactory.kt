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

package org.radarbase.connect.upload.converter

import org.radarbase.connect.upload.api.ContentsDTO

/**
 * Processor for processing the header of CSV file.
 */
interface CsvHeaderProcessorFactory {
    /** Upper case header list. */
    val header: List<String>

    /** Expected file header list (uppercase). */
    open val fileHeader: List<String>

    val fileNameSuffixes: List<String>
        get() = listOf(".csv")

    /**
     * Whether the file contents matches this CSV line processor.
     */
    fun matches(contents: ContentsDTO) = fileNameSuffixes.any {
        contents.fileName.endsWith(it, ignoreCase = true)
    }

    /**
     * Whether the header matches this CSV line processor. The provided header must be in upper
     * case.
     */
    fun matches(header: List<String>): Boolean = header.stream().anyMatch(this.header::contains)

    /**
     * Process the header and return with the same format.
     */
    open fun process(header: List<String>): List<String> = header

}

