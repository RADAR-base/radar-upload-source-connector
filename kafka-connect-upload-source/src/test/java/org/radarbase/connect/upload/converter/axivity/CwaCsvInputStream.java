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
 * CWA Converter
 */

package org.radarbase.connect.upload.converter.axivity;

import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock;
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaReader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock.DATA_EVENT_BUFFER_OVERFLOW;
import static org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock.DATA_EVENT_CHECKSUM_FAIL;
import static org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock.DATA_EVENT_DOUBLE_TAP;
import static org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock.DATA_EVENT_EVENT;
import static org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock.DATA_EVENT_FIFO_OVERFLOW;
import static org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock.DATA_EVENT_RESUME;
import static org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock.DATA_EVENT_SINGLE_TAP;
import static org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock.DATA_EVENT_UNHANDLED_INTERRUPT;

/**
 * A FilterInputStream that converts the binary CWA files into a CSV stream.
 * The output format is:
 *     <code>yyyy-MM-dd HH:mm:ss.SSS,X,Y,Z</code>
 * ...where X, Y, Z are signed floats representing the axis acceleration measured in 'G'.
 *
 * Note: New format files will have an interpolated timestamp.
 *       For old format files, only the timestamp of the containing block is shown for each sample.
 *
 * Example usage:
 * <pre>
 *     // Create a standard file input stream for the binary file
 *     InputStream in = new FileInputStream("CWA-DATA.CWA");
 *
 *     // Create the filter stream to convert it into CSV
 *     CwaCsvInputStream csvIn = new CwaCsvInputStream(in);
 *
 *     // Use a buffered reader to read the CSV line-by-line
 *     BufferedReader filter = new BufferedReader(new InputStreamReader(csvIn));
 *
 *     // While not EOF, read and output the CSV lines
 *     String line;
 *     while ((line = filter.readLine()) != null)
 *         System.out.println(line);
 *
 *     // Close the outermost stream
 *     filter.close();
 * </pre>
 * @author Dan Jackson, Newcastle University
 */
public class CwaCsvInputStream extends FilterInputStream {

    // Block reader
    private final CwaReader cwaReader;

    // Output buffer
    private final static int MAX_OUT_BUFFER = (CwaBlock.MAX_SAMPLES_PER_BLOCK * 128);
    private ByteBuffer outBuffer;
    private final StringBuilder outputStringBuilder;
    public int line;
    private final int firstLine, lineSkip, lineCount, options;
    private short events = 0;

    /** No export options */
    public static final int OPTIONS_NONE = 0x00;
    /** Export option for light values */
    public static final int OPTIONS_LIGHT = 0x01;
    /** Export option for temperature values */
    public static final int OPTIONS_TEMP = 0x02;
    /** Export option for battery values */
    public static final int OPTIONS_BATT = 0x04;
    /** Export option for events values */
    public static final int OPTIONS_EVENTS = 0x08;
    /** Export option for meta-data header */
    public static final int OPTIONS_METADATA = 0x10;

    /**
     * Creates a CwaCsvInputStream object
     * @param inputStream the source input stream
     * @param firstLine the first line to return (0 returns from the first line)
     * @param lineSkip return every nth line (1 returns every line)
     * @param lineCount maximum number of lines to return (-1 for infinite limit)
     * @param options combination of OPTIONS_X flags
     */
    public CwaCsvInputStream(InputStream inputStream, int firstLine, int lineSkip, int lineCount,
            int options) {
        super(inputStream);
        this.firstLine = firstLine;
        this.lineSkip = lineSkip;
        this.lineCount = lineCount;
        this.options = options;
        outputStringBuilder = new StringBuilder(MAX_OUT_BUFFER);
        outBuffer = ByteBuffer.wrap(new byte[MAX_OUT_BUFFER]);
        outBuffer.position(0);
        outBuffer.limit(0);
        line = 0;
        cwaReader = new CwaReader(inputStream);
        try {
            cwaReader.skipNonDataBlocks();
        } catch (IOException e) {
            System.err.println(
                    "EXCEPTION: IOException in CwaCsvInputStream() at: cwaReader.skipNonDataBlocks() -- "
                            + e.getMessage());
        }

        if ((options & OPTIONS_METADATA) != 0) {
            String lineSeparator = System.getProperty("line.separator");
            String prefix = ",,,,,,,,"; //"t,x,y,z,L,T,B,E,NAME,VALUE"

            outputStringBuilder.append(prefix).append("deviceId,").append(cwaReader.getDeviceId())
                    .append(lineSeparator);
            Map<String, String> annotations = cwaReader.getAnnotations();
            for (String key : annotations.keySet()) {
                outputStringBuilder.append(prefix).append(key).append(",")
                        .append(annotations.get(key)).append(lineSeparator);
            }

            // Check if the output buffer is big enough (it should be, but we can create a new one if it wasn't)
            if (outputStringBuilder.length() > outBuffer.capacity()) {
                outBuffer = ByteBuffer.wrap(new byte[outputStringBuilder.length()]);
            }

            // Update the limit and position of the output buffer
            outBuffer.position(0);
            outBuffer.limit(outputStringBuilder.length());

            // Copy string builder to byte array
            int len = outputStringBuilder.length();
            for (int i = 0; i < len; i++) {
                outBuffer.array()[i] = (byte) (outputStringBuilder.charAt(i));
            }

            // Empty string builder
            outputStringBuilder.delete(0, outputStringBuilder.length());
        }
    }

    private void fillOutputBuffer() throws IOException {
        // Peek the next block (temporary copy)
        CwaBlock block;
        for (; ; ) {
            block = cwaReader.peekBlock();
            if (block == null) {
                return;
            }
            if (block.isDataBlock()) {
                break;
            }
            block.invalidate();
        }

        long[] timestamps = block.getTimestampValues();
        String lineSeparator = System.getProperty("line.separator");
        short[] sampleValues = block.getSampleValues();
        int numSamples = block.getNumSamples();
        for (int i = 0; i < numSamples; i++) {
            float x = (float) sampleValues[CwaBlock.NUM_AXES_PER_SAMPLE * i] / 256.0f;
            float y = (float) sampleValues[CwaBlock.NUM_AXES_PER_SAMPLE * i + 1] / 256.0f;
            float z = (float) sampleValues[CwaBlock.NUM_AXES_PER_SAMPLE * i + 2] / 256.0f;

            // Accumulate all events until displayed
            events |= block.getEvents();

            if (line >= firstLine && (line % lineSkip) == 0 && (lineCount < 0
                    || line < lineCount * lineSkip)) {
                //outputStringBuilder.append(timestamps[i]);
                outputStringBuilder.append(getDateString(timestamps[i]));
                outputStringBuilder.append(',').append(x).append(',').append(y).append(',')
                        .append(z);

                if ((options & OPTIONS_LIGHT) != 0) {
                    outputStringBuilder.append(',').append(block.getLight());
                }
                if ((options & OPTIONS_TEMP) != 0) {
                    outputStringBuilder.append(',').append(block.getTemperature());
                }
                if ((options & OPTIONS_BATT) != 0) {
                    outputStringBuilder.append(',').append(block.getBattery());
                }
                if ((options & OPTIONS_EVENTS) != 0) {
                    outputStringBuilder.append(',');
                    if ((events & DATA_EVENT_RESUME) != 0) {
                        outputStringBuilder.append('r');
                    }
                    if ((events & DATA_EVENT_SINGLE_TAP) != 0) {
                        outputStringBuilder.append('s');
                    }
                    if ((events & DATA_EVENT_DOUBLE_TAP) != 0) {
                        outputStringBuilder.append('d');
                    }
                    if ((events & DATA_EVENT_EVENT) != 0) {
                        outputStringBuilder.append('e');
                    }
                    if ((events & DATA_EVENT_FIFO_OVERFLOW) != 0) {
                        outputStringBuilder.append('F');
                    }
                    if ((events & DATA_EVENT_BUFFER_OVERFLOW) != 0) {
                        outputStringBuilder.append('B');
                    }
                    if ((events & DATA_EVENT_UNHANDLED_INTERRUPT) != 0) {
                        outputStringBuilder.append('I');
                    }
                    if ((events & DATA_EVENT_CHECKSUM_FAIL) != 0) {
                        outputStringBuilder.append('X');
                    }
                    events = 0x00;
                }

                outputStringBuilder.append(lineSeparator);
            }

            line++;
        }

        // Check if the output buffer is big enough (it should be, but we can create a new one if it wasn't)
        if (outputStringBuilder.length() > outBuffer.capacity()) {
            outBuffer = ByteBuffer.wrap(new byte[outputStringBuilder.length()]);
        }

        // Update the limit and position of the output buffer
        outBuffer.position(0);
        outBuffer.limit(outputStringBuilder.length());

        // Copy string builder to byte array
        int len = outputStringBuilder.length();
        for (int i = 0; i < len; i++) {
            outBuffer.array()[i] = (byte) (outputStringBuilder.charAt(i));
        }

        // Empty string builder
        outputStringBuilder.delete(0, outputStringBuilder.length());

        // Invalidate the next block (we will read the following block next time)
        block.invalidate();
    }

    // Reads up to len bytes of data from this input stream into an array of bytes.
    public int read(byte[] b, int off, int len) throws IOException {
        // If out buffer empty, re-fill
        if (outBuffer.remaining() == 0) {
            fillOutputBuffer();
            // If out buffer still empty, must be eof
            if (outBuffer.remaining() == 0) {
                return -1;
            }
        }

        // At most, read to the end of the output buffer
        if (len > outBuffer.remaining()) {
            len = outBuffer.remaining();
        }

        // Read the bytes from the buffer
        outBuffer.get(b, off, len);
        return len;
    }

    // Skips over and discards n bytes of data from the input stream.
    public long skip(long length) throws IOException {
        long remaining = length;
        while (remaining > 0) {
            int n = read(null, 0, (int) remaining);
            if (n == 0) {
                break;
            }
            remaining -= n;
        }
        return 0;
    }

    // Returns the number of bytes that can be read from this input stream without blocking.
    public int available() {
        return outBuffer.remaining();
    }

    // Reads up to byte.length bytes of data from this input stream into an array of bytes.
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    // Single byte buffer
    private byte[] singleByteBuffer = new byte[1];

    // Reads the next byte of data from this input stream.
    @Override
    public int read() throws IOException {
        if (read(singleByteBuffer, 0, 1) != 1) {
            return -1;
        }
        return singleByteBuffer[0];
    }

    // Closes this input stream and releases any system resources associated with the stream.
    public void close() throws IOException {
        cwaReader.close();
    }

    // Tests if this input stream supports the mark and reset methods.
    public boolean markSupported() {
        return false;
    }

    // Marks the current position in this input stream.
    public void mark(int readlimit) { /*throw new IOException("Mark not supported.");*/ }

    // Repositions this stream to the position at the time the mark method was last called on this input stream.
    public void reset() throws IOException {
        throw new IOException("Reset to mark not supported.");
    }

    /**
     * Standard CWA date formatting string
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private static DateFormat dateFormat = null;

    /**
     * @param timespan Timespan since 1/1/1970 in milliseconds
     * @return Standard formatting for the supplied date
     */
    private static String getDateString(long timespan) {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat(DATE_FORMAT);
        }
        return dateFormat.format(new Date(timespan));
    }
}
