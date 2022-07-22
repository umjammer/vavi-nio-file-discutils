//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package discUtils.diagnostics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import discUtils.core.internal.LocalFileLocator;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamWriter;


/**
 * Stream wrapper that traces all read and write activity.
 */
public final class TracingStream extends Stream {

    private Stream wrapped;

    private Ownership ownsWrapped;

    private List<StreamTraceRecord> records;

    private boolean active;

    private boolean captureStack;

    @SuppressWarnings("unused")
    private boolean captureStackFileDetails = false;

    private boolean traceReads;

    private boolean traceWrites = true;

    private StreamWriter fileOut;

    /**
     * Creates a new instance, wrapping an existing stream.
     *
     * @param toWrap The stream to wrap
     * @param ownsWrapped Indicates if this stream controls toWrap's lifetime
     */
    public TracingStream(Stream toWrap, Ownership ownsWrapped) {
        wrapped = toWrap;
        this.ownsWrapped = ownsWrapped;
        records = new ArrayList<>();
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        if (ownsWrapped == Ownership.Dispose && wrapped != null) {
            wrapped.close();
        }

        wrapped = null;
        if (fileOut != null) {
            fileOut.close();
        }

        fileOut = null;
    }

    /**
     * Starts tracing stream activity.
     */
    public void start() {
        active = true;
    }

    /**
     * Stops tracing stream activity.
     * Old trace records are not discarded, use
     * {@code Start}
     * to resume the trace
     */
    public void stop() {
        active = false;
    }

    /**
     * Resets tracing on the stream.
     *
     * @param start Whether to enable or disable tracing after this method
     *            completes
     */
    public void reset(boolean start) {
        active = false;
        records.clear();
        if (start) {
            start();
        }
    }

    /**
     * Gets and sets whether to capture stack traces for every read/write
     */
    public boolean getCaptureStackTraces() {
        return captureStack;
    }

    public void setCaptureStackTraces(boolean value) {
        captureStack = value;
    }

    /**
     * Gets and sets whether to trace read activity (default is false).
     */
    public boolean getTraceReads() {
        return traceReads;
    }

    public void setTraceReads(boolean value) {
        traceReads = value;
    }

    /**
     * Gets and sets whether to trace write activity (default is true).
     */
    public boolean getTraceWrites() {
        return traceWrites;
    }

    public void setTraceWrites(boolean value) {
        traceWrites = value;
    }

    /**
     * Directs trace output to a file as well as storing internally.
     *
     * @param path The path to the file.Call this method after tracing has
     *            started to migrate to a new
     *            output file.
     */
    public void writeToFile(String path) {
        if (fileOut != null) {
            try {
                fileOut.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            fileOut = null;
        }

        if (path != null && !path.isEmpty()) {
            LocalFileLocator locator = new LocalFileLocator("");
            fileOut = new StreamWriter(locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.ReadWrite));
        }
    }

    /**
     * Gets a log of all recorded stream activity.
     */
    public List<StreamTraceRecord> getLog() {
        return records;
    }

    /**
     * Gets an indication as to whether the stream can be read.
     */
    public boolean canRead() {
        return wrapped.canRead();
    }

    /**
     * Gets an indication as to whether the stream position can be changed.
     */
    public boolean canSeek() {
        return wrapped.canSeek();
    }

    /**
     * Gets an indication as to whether the stream can be written to.
     */
    public boolean canWrite() {
        return wrapped.canWrite();
    }

    /**
     * Flushes the stream.
     */
    public void flush() {
        wrapped.flush();
    }

    /**
     * Gets the length of the stream.
     */
    public long getLength() {
        return wrapped.getLength();
    }

    /**
     * Gets and sets the current stream position.
     */
    public long getPosition() {
        return wrapped.getPosition();
    }

    public void setPosition(long value) {
        wrapped.setPosition(value);
    }

    /**
     * Reads data from the stream.
     *
     * @param buffer The buffer to fill
     * @param offset The buffer offset to start from
     * @param count The number of bytes to read
     * @return The number of bytes read
     */
    public int read(byte[] buffer, int offset, int count) {
        long position = wrapped.getPosition();
        try {
            int result = wrapped.read(buffer, offset, count);
            if (active && traceReads) {
                createAndAddRecord("READ", position, count, result);
            }

            return result;
        } catch (Exception e) {
            if (active && traceReads) {
                createAndAddRecord("READ", position, count, e);
            }

            throw e;
        }
    }

    /**
     * Moves the stream position.
     *
     * @param offset The origin-relative location
     * @param origin The base location
     * @return The new absolute stream position
     */
    public long seek(long offset, SeekOrigin origin) {
        return wrapped.seek(offset, origin);
    }

    /**
     * Sets the length of the stream.
     *
     * @param value The new length
     */
    public void setLength(long value) {
        wrapped.setLength(value);
    }

    /**
     * Writes data to the stream at the current location.
     *
     * @param buffer The data to write
     * @param offset The first byte to write from buffer
     * @param count The number of bytes to write
     */
    public void write(byte[] buffer, int offset, int count) {
        long position = wrapped.getPosition();
        try {
            wrapped.write(buffer, offset, count);
            if (active && traceWrites) {
                createAndAddRecord("WRITE", position, count);
            }

        } catch (Exception e) {
            if (active && traceWrites) {
                createAndAddRecord("WRITE", position, count, e);
            }

            throw e;
        }
    }

    private StreamTraceRecord createAndAddRecord(String activity, long position, long count) {
        return createAndAddRecord(activity, position, count, 0, null);
    }

    private StreamTraceRecord createAndAddRecord(String activity, long position, long count, int result) {
        return createAndAddRecord(activity, position, count, result, null);
    }

    private StreamTraceRecord createAndAddRecord(String activity, long position, long count, Exception e) {
        return createAndAddRecord(activity, position, count, -1, e);
    }

    private StreamTraceRecord createAndAddRecord(String activity, long position, long count, int result, Exception ex) {
        try {
            // Note: Not sure about the 'ex' parameter to StackTrace, but the new StackTrace does not accept a frameCount
            StackTraceElement[] trace = captureStack ? ex.getStackTrace(/*captureStackFileDetails*/) : null;
            StreamTraceRecord record = new StreamTraceRecord(records.size(), activity, position, trace);
            record.setCountArg(count);
            record.setResult(result);
            record.setExceptionThrown(ex);
            records.add(record);
            if (fileOut != null) {
                fileOut.writeLine(record);
                if (trace != null) {
                    fileOut.write(Arrays.toString(trace));
                }

                if (ex != null) {
                    fileOut.writeLine(ex);
                }

                fileOut.flush();
            }

            return record;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }
}
