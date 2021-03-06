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

package DiscUtils.Diagnostics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Streams.Util.Ownership;
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
    private Stream _wrapped;

    private Ownership _ownsWrapped;

    private List<StreamTraceRecord> _records;

    private boolean _active;

    private boolean _captureStack;

    @SuppressWarnings("unused")
    private boolean _captureStackFileDetails = false;

    private boolean _traceReads;

    private boolean _traceWrites = true;

    private StreamWriter _fileOut;

    /**
     * Creates a new instance, wrapping an existing stream.
     *
     * @param toWrap The stream to wrap
     * @param ownsWrapped Indicates if this stream controls toWrap's lifetime
     */
    public TracingStream(Stream toWrap, Ownership ownsWrapped) {
        _wrapped = toWrap;
        _ownsWrapped = ownsWrapped;
        _records = new ArrayList<>();
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        if (_ownsWrapped == Ownership.Dispose && _wrapped != null) {
            _wrapped.close();
        }

        _wrapped = null;
        if (_fileOut != null) {
            _fileOut.close();
        }

        _fileOut = null;
    }

    /**
     * Starts tracing stream activity.
     */
    public void start() {
        _active = true;
    }

    /**
     * Stops tracing stream activity.
     * Old trace records are not discarded, use
     * {@code Start}
     * to resume the trace
     */
    public void stop() {
        _active = false;
    }

    /**
     * Resets tracing on the stream.
     *
     * @param start Whether to enable or disable tracing after this method
     *            completes
     */
    public void reset(boolean start) {
        _active = false;
        _records.clear();
        if (start) {
            start();
        }
    }

    /**
     * Gets and sets whether to capture stack traces for every read/write
     */
    public boolean getCaptureStackTraces() {
        return _captureStack;
    }

    public void setCaptureStackTraces(boolean value) {
        _captureStack = value;
    }

    /**
     * Gets and sets whether to trace read activity (default is false).
     */
    public boolean getTraceReads() {
        return _traceReads;
    }

    public void setTraceReads(boolean value) {
        _traceReads = value;
    }

    /**
     * Gets and sets whether to trace write activity (default is true).
     */
    public boolean getTraceWrites() {
        return _traceWrites;
    }

    public void setTraceWrites(boolean value) {
        _traceWrites = value;
    }

    /**
     * Directs trace output to a file as well as storing internally.
     *
     * @param path The path to the file.Call this method after tracing has
     *            started to migrate to a new
     *            output file.
     */
    public void writeToFile(String path) {
        if (_fileOut != null) {
            try {
                _fileOut.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            _fileOut = null;
        }

        if (path != null && !path.isEmpty()) {
            LocalFileLocator locator = new LocalFileLocator("");
            _fileOut = new StreamWriter(locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.ReadWrite));
        }
    }

    /**
     * Gets a log of all recorded stream activity.
     */
    public List<StreamTraceRecord> getLog() {
        return _records;
    }

    /**
     * Gets an indication as to whether the stream can be read.
     */
    public boolean canRead() {
        return _wrapped.canRead();
    }

    /**
     * Gets an indication as to whether the stream position can be changed.
     */
    public boolean canSeek() {
        return _wrapped.canSeek();
    }

    /**
     * Gets an indication as to whether the stream can be written to.
     */
    public boolean canWrite() {
        return _wrapped.canWrite();
    }

    /**
     * Flushes the stream.
     */
    public void flush() {
        _wrapped.flush();
    }

    /**
     * Gets the length of the stream.
     */
    public long getLength() {
        return _wrapped.getLength();
    }

    /**
     * Gets and sets the current stream position.
     */
    public long getPosition() {
        return _wrapped.getPosition();
    }

    public void setPosition(long value) {
        _wrapped.setPosition(value);
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
        long position = _wrapped.getPosition();
        try {
            int result = _wrapped.read(buffer, offset, count);
            if (_active && _traceReads) {
                createAndAddRecord("READ", position, count, result);
            }

            return result;
        } catch (Exception e) {
            if (_active && _traceReads) {
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
        return _wrapped.seek(offset, origin);
    }

    /**
     * Sets the length of the stream.
     *
     * @param value The new length
     */
    public void setLength(long value) {
        _wrapped.setLength(value);
    }

    /**
     * Writes data to the stream at the current location.
     *
     * @param buffer The data to write
     * @param offset The first byte to write from buffer
     * @param count The number of bytes to write
     */
    public void write(byte[] buffer, int offset, int count) {
        long position = _wrapped.getPosition();
        try {
            _wrapped.write(buffer, offset, count);
            if (_active && _traceWrites) {
                createAndAddRecord("WRITE", position, count);
            }

        } catch (Exception e) {
            if (_active && _traceWrites) {
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
            StackTraceElement[] trace = _captureStack ? ex.getStackTrace(/*_captureStackFileDetails*/) : null;
            StreamTraceRecord record = new StreamTraceRecord(_records.size(), activity, position, trace);
            record.setCountArg(count);
            record.setResult(result);
            record.setExceptionThrown(ex);
            _records.add(record);
            if (_fileOut != null) {
                _fileOut.writeLine(record);
                if (trace != null) {
                    _fileOut.write(Arrays.toString(trace));
                }

                if (ex != null) {
                    _fileOut.writeLine(ex);
                }

                _fileOut.flush();
            }

            return record;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }
}
