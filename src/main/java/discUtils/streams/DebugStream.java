/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.streams;

import java.io.IOException;
import java.util.logging.Level;

import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import vavi.util.Debug;


/**
 * DebugStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/12/08 umjammer initial version <br>
 */
public class DebugStream extends Stream {

    private Stream wrapped;

    public DebugStream(Stream toWrap) {
        wrapped = toWrap;
try {
 wrapped.position(0);
 Debug.println("wrapped: " + wrapped.getClass() + "@" + wrapped.hashCode());
} catch (Throwable t) {
 Debug.println(Level.SEVERE, "wrapped null stream?");
 t.printStackTrace();
}
    }

    @Override public boolean canRead() {
        return wrapped.canRead();
    }

    @Override public boolean canSeek() {
        return wrapped.canSeek();
    }

    @Override public boolean canWrite() {
        return wrapped.canWrite();
    }

    @Override public long getLength() {
        return wrapped.getLength();
    }

    @Override public long position() {
        return wrapped.position();
    }

    @Override public void position(long value) {
try {
        wrapped.position(value);
} catch (Throwable t) {
 Debug.println(wrapped.getClass() + "@" + wrapped.hashCode() + ", " + t);
 throw t;
}
    }

    @Override public void flush() {
        wrapped.flush();
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        return wrapped.read(buffer, offset, count);
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        return wrapped.seek(offset, origin);
    }

    @Override public void setLength(long value) {
        wrapped.setLength(value);
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        wrapped.write(buffer, offset, count);
    }

    @Override public void close() throws IOException {
new Exception("*** DUMMY ***").printStackTrace();
        wrapped.close();
        wrapped = null;
    }
}
