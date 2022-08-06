/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.emu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import vavi.util.Debug;


/**
 * DiskStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/07/28 umjammer initial version <br>
 */
public class DiskStream extends SparseStream {

    private boolean atEof;

    private final vavi.emu.disk.Disk disk;

    private Stream fileStream;

    private boolean isDisposed;

    private final Ownership ownsStream;

    private boolean writeNotified;

    public DiskStream(Stream fileStream, Ownership ownsStream, vavi.emu.disk.Disk disk) {
        this.fileStream = fileStream;
        this.disk = disk;

        this.ownsStream = ownsStream;
    }

    public boolean canRead() {
        checkDisposed();
        return true;
    }

    public boolean canSeek() {
        checkDisposed();
        return true;
    }

    public boolean canWrite() {
        checkDisposed();
        return fileStream.canWrite();
    }

    public List<StreamExtent> getExtents() {
        if (true)
            throw new UnsupportedOperationException("not implemented yet");

        List<StreamExtent> extents = new ArrayList<>();

        long blockSize = disk.getSctorSize();
        int i = 0;
        extents.add(new StreamExtent(blockSize, i * blockSize));

        return extents;
    }

    public long getLength() {
        checkDisposed();
        return disk.getLength();
    }

    public long getPosition() {
        checkDisposed();
//Debug.printf("GETPOS: %08x", (fileStream.getPosition() - disk.getOffset()));
        return fileStream.getPosition() - disk.getOffset();
    }

    public void setPosition(long value) {
        checkDisposed();
//Debug.printf("SETPOS: %08x", value);
        fileStream.setPosition(value + disk.getOffset());
        atEof = false;
    }

    public BiConsumer<Object, Object[]> writeOccurred;

    public void flush() {
        checkDisposed();
    }

    public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
//new Exception().printStackTrace();
//Debug.printf("READ: %08x, %d, %d", fileStream.getPosition(), offset, count);
        if (atEof || fileStream.getPosition() > disk.getLength()) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
        }

        if (fileStream.getPosition() == disk.getLength()) {
            atEof = true;
            return 0;
        }

        StreamUtilities.readExact(fileStream, buffer, offset, count);

        return count;
    }

    public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += fileStream.getPosition();
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += disk.getLength();
        }

        atEof = false;

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }
        fileStream.setPosition(disk.getOffset() + effectiveOffset);
        return fileStream.getPosition() - disk.getOffset();
    }

    public void setLength(long value) {
        checkDisposed();
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        if (true)
            throw new UnsupportedOperationException("not implemented yet");

        checkDisposed();

        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Attempt to write to read-only stream");
        }

        if (count < 0) {
            throw new IndexOutOfBoundsException("Attempt to write negative number of bytes (count)");
        }

        if (atEof || fileStream.getPosition() + count > disk.getLength()) {
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

    public void close() throws IOException {
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
