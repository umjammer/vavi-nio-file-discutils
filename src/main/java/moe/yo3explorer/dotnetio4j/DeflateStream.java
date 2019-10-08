/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.IOException;


/**
 * StreamOutputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/09/30 umjammer initial version <br>
 */
public class DeflateStream extends Stream {

    public enum CompressionMode {
        Decompress,
        Compress
    }

    Stream stream;

    /**
     * @param stream
     * @param mode
     * @param leaveOpen
     */
    public DeflateStream(Stream stream, CompressionMode mode, boolean leaveOpen) {
        this.stream = stream;
    }

    /**
     * @param stream
     * @param mode
     */
    public DeflateStream(Stream stream, CompressionMode mode) {
        this(stream, mode, false);
    }

    @Override
    public boolean canRead() {
        return stream.canRead();
    }

    @Override
    public boolean canSeek() {
        return stream.canSeek();
    }

    @Override
    public boolean canWrite() {
        return stream.canWrite();
    }

    @Override
    public long getLength() {
        return stream.getLength();
    }

    @Override
    public long getPosition() {
        return stream.getPosition();
    }

    @Override
    public void setPosition(long value) {
        stream.setPosition(value);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public void flush() {
        stream.flush();
    }

    @Override
    public long seek(long offset, SeekOrigin origin) {
        return stream.seek(offset, origin);
    }

    @Override
    public void setLength(long value) {
        stream.setLength(value);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) {
        return stream.read(buffer, offset, length);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) {
        stream.write(buffer, offset, count);
    }
}

/* */
