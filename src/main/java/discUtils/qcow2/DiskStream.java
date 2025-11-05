/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.qcow2;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.github.qcow2.Qcow2.Image;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.Ownership;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;

import static java.lang.System.getLogger;


/**
 * DiskStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/10/01 umjammer initial version <br>
 */
public class DiskStream extends SparseStream {

    private static final Logger logger = getLogger(DiskStream.class.getName());

    private boolean atEof;

    private final Image disk;

    private Stream fileStream;

    private boolean isDisposed;

    private final Ownership ownsStream;

    private boolean writeNotified;

    private long position;

    public DiskStream(Stream fileStream, Ownership ownsStream, Image disk) {
        this.fileStream = fileStream;
        this.disk = disk;

        this.ownsStream = ownsStream;
        this.position = 0;
    }

    @Override public boolean canRead() {
        checkDisposed();
        return true;
    }

    @Override public boolean canSeek() {
        checkDisposed();
        return true;
    }

    @Override public boolean canWrite() {
        checkDisposed();
        return fileStream.canWrite();
    }

    @Override public List<StreamExtent> getExtents() {
        if (true)
            throw new UnsupportedOperationException("not implemented yet");

        List<StreamExtent> extents = new ArrayList<>();

        long blockSize = -1;
        int i = 0;
        extents.add(new StreamExtent(blockSize, i * blockSize));

        return extents;
    }

    @Override public long getLength() {
        checkDisposed();
        return disk.getSize();
    }

    @Override public long position() {
        checkDisposed();
logger.log(Level.TRACE, "get position: %08x".formatted(position));
        return position;
    }

    @Override public void position(long value) {
        checkDisposed();
logger.log(Level.TRACE, "set position: %08x".formatted(value));
        this.position = value;
        atEof = false;
    }

    public BiConsumer<Object, Object[]> writeOccurred;

    @Override public void flush() {
        checkDisposed();
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
//new Exception().printStackTrace(System.err);
logger.log(Level.TRACE, "READ: %08x, %d, %d".formatted(position, offset, count));
        if (atEof || position > disk.getSize()) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
        }

        if (position == disk.getSize()) {
            atEof = true;
            return 0;
        }

        try {
            byte[] b = new byte[count];
            disk.readAt(b, position);
            System.arraycopy(b, 0, buffer, offset, count);
            position += count;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        return count;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == dotnet4j.io.SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += disk.getSize();
        }

        atEof = false;

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }
        position = offset + effectiveOffset;
        return position - offset;
    }

    @Override public void setLength(long value) {
        checkDisposed();
        throw new UnsupportedOperationException();
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        if (true)
            throw new UnsupportedOperationException("not implemented yet");

        checkDisposed();

        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Attempt to write to read-only stream");
        }

        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to write negative number of bytes (count)");
        }

        if (atEof || position + count > disk.getSize()) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to write beyond end of file");
        }

        // On first write, notify event listeners - they just get to find out that some
        // write occurred, not about each write.
        if (!writeNotified) {
            onWriteOccurred();
            writeNotified = true;
        }

        // Existing block, simply overwrite the existing data
        fileStream.write(buffer, offset,  count);
    }

    @Override public void close() throws IOException {
        isDisposed = true;
        if (ownsStream == Ownership.Dispose && fileStream != null) {
            fileStream.close();
            fileStream = null;
        }
    }

    protected void onWriteOccurred() {
        if (writeOccurred != null) {
            writeOccurred.accept(this, null);
        }
    }

    private void checkDisposed() {
        if (isDisposed) {
            throw new dotnet4j.io.IOException("DiskStream: Attempt to use disposed stream");
        }
    }
}
