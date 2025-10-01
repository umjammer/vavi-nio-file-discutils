/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.qcow2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.github.qcow2.Qcow2.Image;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * DiskStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/10/01 umjammer initial version <br>
 */
public class DiskStream extends SparseStream {

    private boolean atEof;

    private final Image disk;

    private Stream fileStream;

    private boolean isDisposed;

    private final Ownership ownsStream;

    private boolean writeNotified;

    private long offset;

    public DiskStream(Stream fileStream, Ownership ownsStream, Image disk) {
        this.fileStream = fileStream;
        this.disk = disk;

        this.ownsStream = ownsStream;
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
//logger.log(Level.DEBUG, "GETPOS: %08x".formatted((fileStream.position() - offset)));
        return fileStream.position() - offset;
    }

    @Override public void position(long value) {
        checkDisposed();
//logger.log(Level.DEBUG, "SETPOS: %08x".formatted(value));
        fileStream.position(value + offset);
        atEof = false;
    }

    public BiConsumer<Object, Object[]> writeOccurred;

    @Override public void flush() {
        checkDisposed();
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
//new Exception().printStackTrace(System.err);
//logger.log(Level.DEBUG, "READ: %08x, %d, %d".formatted(fileStream.position(), offset, count));
        if (atEof || fileStream.position() > disk.getSize()) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
        }

        if (fileStream.position() == disk.getSize()) {
            atEof = true;
            return 0;
        }

        StreamUtilities.readExact(fileStream, buffer, offset, count);

        return count;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += fileStream.position();
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += disk.getSize();
        }

        atEof = false;

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }
        fileStream.position(offset + effectiveOffset);
        return fileStream.position() - offset;
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

        if (atEof || fileStream.position() + count > disk.getSize()) {
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
