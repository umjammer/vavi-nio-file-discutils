/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package dotnet4j.io.compat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * JavaNIOStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/07/28 umjammer initial version <br>
 */
public class JavaNIOStream extends Stream {

    protected final boolean leaveOpen;

    protected SeekableByteChannel sbc;

    /** */
    public JavaNIOStream(SeekableByteChannel sbc) {
        this(sbc, false);
    }

    /** */
    public JavaNIOStream(SeekableByteChannel sbc, boolean leaveOpen) {
        this.sbc = sbc;
        this.leaveOpen = leaveOpen;
    }

    @Override
    public boolean canRead() {
        return sbc != null;
    }

    @Override
    public boolean canSeek() {
        return false;
    }

    @Override
    public boolean canWrite() {
        return sbc != null;
    }

    @Override
    public long getLength() {
        try {
            return sbc.size();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public long position() {
        try {
            return sbc.position();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public void position(long value) {
        try {
            sbc.position(value);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (!leaveOpen) {
            if (sbc != null) {
                sbc.close();
                sbc = null;
            }
        }
    }

    @Override
    public void flush() {
        if (sbc == null) {
            throw new dotnet4j.io.IOException("closed");
        }
    }

    @Override
    public long seek(long offset, SeekOrigin origin) {
        try {
            switch (origin) {
            case Begin:
                sbc.position(offset);
                break;
            case Current:
                sbc.position(sbc.position() + offset);
                break;
            case End:
                sbc.position(sbc.size() - offset);
                break;
            }
            return sbc.position();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public void setLength(long value) {
        try {
            sbc.truncate(value);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) {
        if (sbc == null) {
            throw new dotnet4j.io.IOException("closed");
        }

        try {
//logger.log(Level.DEBUG, buffer.length + ", " + offset + ", " + length + ", " + is.available());
            int r = sbc.read(ByteBuffer.wrap(buffer, offset, length));
//logger.log(Level.DEBUG, StringUtil.getDump(buffer, 16));
            if (r == -1) {
//logger.log(Level.DEBUG, "EOF");
                return 0; // C# Spec.
            }
//logger.log(Level.DEBUG, "position: " + position);
            return r;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public int readByte() {
        if (sbc == null) {
            throw new dotnet4j.io.IOException("closed");
        }

        return super.readByte();
    }

    @Override
    public void write(byte[] buffer, int offset, int count) {
        if (sbc == null) {
            throw new dotnet4j.io.IOException("closed");
        }

        try {
//logger.log(Level.DEBUG, "w: " + count + ", " + os);
            sbc.write(ByteBuffer.wrap(buffer, offset, count));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public void writeByte(byte value) {
        if (sbc == null) {
            throw new dotnet4j.io.IOException("closed");
        }

        super.writeByte(value);
    }
}
