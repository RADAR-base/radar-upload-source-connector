/*
 * Copyright (c) 2009-2018, Newcastle University, UK.
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * CWA File Reader
 */
package org.radarbase.connect.upload.converter.axivity.newcastle

import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder

/**
 * A binary CWA file reader class used by CwaCsvInputStream.
 * @author Dan Jackson, Newcastle University
 */
class CwaReader(
    /** Wrapped input stream  */
    private val inputStream: InputStream
) {
    /** Block  */
    private val currentBlock: CwaBlock = CwaBlock()

    var sessionId = -1
        private set

    /** @return Device ID
     */
    var deviceId = -1
        private set

    private val _annotations: MutableMap<String, String> = HashMap()

    val annotations: Map<String, String>
        get() = _annotations.toMap()

    /**
     * Closes the wrapped input stream
     * @throws IOException if closing the input stream fails
     */
    @Throws(IOException::class)
    fun close() {
        inputStream.close()
    }

    /**
     * Returns a *temporary* preview of the next block without consuming it.
     * The object contents are mutable (for efficiency) and is only valid until the next read/peek call.
     * @return The current block, null if end of file.
     * @throws IOException if reading the input stream fails
     */
    @Throws(IOException::class)
    fun peekBlock(): CwaBlock? {
        // If no block buffered, read a new block
        if (!currentBlock.isValid && !currentBlock.readFromInputStream(inputStream)) {
            return null
        }
        if (currentBlock.isDataBlock && currentBlock.sessionId != sessionId) {
            currentBlock.invalidate()
        }
        // Return current block
        return currentBlock
    }

    /**
     * Reads a block of data into memory.
     * @throws IOException if reading the input stream fails
     */
    @Throws(IOException::class)
    fun readBlock() {
        // If no block buffered, read a new block
        if (!currentBlock.isValid) {
            currentBlock.readFromInputStream(inputStream)
        }
        // If invalid
        if (!currentBlock.isValid) {
            return
        }
        // Invalidate the block
        currentBlock.invalidate()
    }

    /**
     * Consumes header and null blocks
     * @throws IOException if data could not be read
     */
    @Throws(IOException::class)
    fun skipNonDataBlocks() {
        while (true) {
            // Peek at the next block
            val block = peekBlock() ?: break
            // If EOF, finished (no data)
            // Determine type of block
            val blockType = block.blockType

            // If header block, read contents
            if (blockType == CwaBlock.BLOCK_HEADER) {
                // Process fist sector (metadata annotations)
                sessionId = block.sessionId
                deviceId = block.deviceId.toInt()

                val metadataString = buildString(960) {
                    for (i in 64 until 512) {
                        val c = block.buffer()[i].toInt()
                        if (c != ' '.code && c >= 0 && c < 128) {
                            append(if (c == '?'.code) '&' else c.toChar())
                        }
                    }
                    append('&')

                    // Process second sector (edited metadata annotations)
                    block.invalidate()
                    readBlock()

                    for (i in 0 until 512) {
                        val c = block.buffer()[i].toInt()
                        if (c > ' '.code && c < 128) {
                            append(if (c == '?'.code) '&' else c.toChar())
                        }
                    }
                }

                metadataString.splitToSequence('&')
                    .filter { it.isNotEmpty() }
                    .forEach { pair ->
                        val i = pair.indexOf('=')
                        if (i <= 0 || i == pair.length - 1) return@forEach

                        val name = pair.substring(0, i).urlDecode()
                        val value = pair.substring(i + 1).urlDecode()
                        _annotations[labelMap.getOrDefault(name, name)] = value
                    }
            } else if (blockType == CwaBlock.BLOCK_DATA) {
                break // exit loop if data seen
            }

            // If not data then consume block
            readBlock()
        }
    }

    companion object {
        private fun String.urlDecode(encoding: String = "UTF-8") =
            URLDecoder.decode(this, encoding)

        private val labelMap = buildMap {
            // At device set-up time
            put("_c", "studyCentre")
            put("_s", "studyCode")
            put("_i", "investigator")
            put("_x", "exerciseCode")
            put("_v", "volunteerNum")
            put("_p", "bodyLocation")
            put("_so", "setupOperator")
            put("_n", "notes")
            // At retrieval time
            put("_b", "startTime")
            put("_e", "endTime")
            put("_ro", "recoveryOperator")
            put("_r", "retrievalTime")
            put("_co", "comments")
        }
    }
}
