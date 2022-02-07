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

package org.radarbase.connect.upload.converter.axivity.newcastle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A binary CWA file reader class used by CwaCsvInputStream.
 * @author Dan Jackson, Newcastle University
 */
public class CwaReader {

    /** Wrapped input stream */
    private final InputStream inputStream;

    /** Block */
    private final CwaBlock currentBlock;

    private int sessionId = -1;
    private int deviceId = -1;
    private final Map<String, String> annotations = new HashMap<>();

    /** @return Device ID */
    public int getDeviceId() {
        return deviceId;
    }

    public int getSessionId() {
        return sessionId;
    }

    /**
     * Creates a CwaReader object
     * @param inputStream the source input stream
     */
    public CwaReader(InputStream inputStream) {
        this.inputStream = inputStream;
        currentBlock = new CwaBlock();
    }

    /**
     * Closes the wrapped input stream
     * @throws IOException if closing the input stream fails
     */
    public void close() throws IOException {
        inputStream.close();
    }

    /**
     * @return metadata annotations
     */
    public Map<String, String> getAnnotations() {
        return annotations;
    }


    /**
     * Returns a *temporary* preview of the next block without consuming it.
     * The object contents are mutable (for efficiency) and is only valid until the next read/peek call.
     * @return The current block, null if end of file.
     * @throws IOException if reading the input stream fails
     */
    public CwaBlock peekBlock() throws IOException {
        // If no block buffered, read a new block
        if (!currentBlock.isValid()) {
            if (!currentBlock.readFromInputStream(inputStream)) {
                return null;
            }
        }
        if (currentBlock.isDataBlock() && currentBlock.getSessionId() != sessionId) {
            currentBlock.invalidate();
        }
        // Return current block
        return currentBlock;
    }

    /**
     * Reads a block of data into memory.
     * @throws IOException if reading the input stream fails
     */
    public void readBlock() throws IOException {
        // If no block buffered, read a new block
        if (!currentBlock.isValid()) {
            currentBlock.readFromInputStream(inputStream);
        }
        // If invalid
        if (!currentBlock.isValid()) {
            return;
        }
        // Invalidate the block
        currentBlock.invalidate();
    }

    /**
     * Consumes header and null blocks
     * @throws IOException if data could not be read
     */
    public void skipNonDataBlocks() throws IOException {
        for (; ; ) {
            // Peek at the next block
            CwaBlock block = peekBlock();
            // If EOF, finished (no data)
            if (block == null) {
                break;
            }
            // Determine type of block
            short blockType = block.getBlockType();

            // If header block, read contents
            if (blockType == CwaBlock.BLOCK_HEADER) {
                Map<String, String> labelMap = new HashMap<>();
                // At device set-up time
                labelMap.put("_c", "studyCentre");
                labelMap.put("_s", "studyCode");
                labelMap.put("_i", "investigator");
                labelMap.put("_x", "exerciseCode");
                labelMap.put("_v", "volunteerNum");
                labelMap.put("_p", "bodyLocation");
                labelMap.put("_so", "setupOperator");
                labelMap.put("_n", "notes");
                // At retrieval time
                labelMap.put("_b", "startTime");
                labelMap.put("_e", "endTime");
                labelMap.put("_ro", "recoveryOperator");
                labelMap.put("_r", "retrievalTime");
                labelMap.put("_co", "comments");

                // Process fist sector (metadata annotations)
                sessionId = block.getSessionId();
                deviceId = block.getDeviceId();
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < 448; i++) {
                    char c = (char) block.buffer().get(64 + i);
                    if (c != ' ' && c >= 0 && c < 128) {
                        if (c == '?') {
                            c = '&';
                        }
                        sb.append(c);
                    }
                }

                sb.append('&');

                // Process second sector (edited metadata annotations)
                block.invalidate();
                readBlock();

                for (int i = 0; i < 512; i++) {
                    char c = (char) block.buffer().get(i);
                    if (c != ' ' && c >= 32 && c < 128) {
                        if (c == '?') {
                            c = '&';
                        }
                        sb.append(c);
                    }
                }

                String metadataString = sb.toString().trim();
                String[] pairs = metadataString.split("&");

                for (String pair : pairs) {
                    int i = pair.indexOf('=');
                    String name = pair;
                    String value = "";
                    if (i >= 0) {
                        name = pair.substring(0, i);
                        value = pair.substring(i + 1);
                    }
                    if (value.trim().length() > 0 && name.trim().length() > 0) {
                        name = URLDecoder.decode(name, "UTF-8");
                        value = URLDecoder.decode(value, "UTF-8");

                        if (labelMap.containsKey(name)) {
                            name = labelMap.get(name);
                        }

                        annotations.put(name, value);
                }
                }
            }
            // If first item of data, peek at contents then exit loop
            else if (blockType == CwaBlock.BLOCK_DATA) {
                break;        // exit loop if data seen
            }

            // If not data then consume block
            readBlock();
        }
    }
}
