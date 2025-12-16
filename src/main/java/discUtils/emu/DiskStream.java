/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.emu;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import vavi.emu.disk.phisical.D88;
import vavi.util.StringUtil;


/**
 * DiskStream.
 *
 * TODO separate D88(CHD) dedicated stream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/11/05 umjammer initial version <br>
 */
public class DiskStream extends SparseStream {

    private static final Logger logger = System.getLogger(DiskStream.class.getName());

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

        long blockSize = disk.getSectorSize();
        int i = 0;
        extents.add(new StreamExtent(blockSize, i * blockSize));

        return extents;
    }

    @Override public long getLength() {
        checkDisposed();
        return disk.getLength();
    }

    @Override public long position() {
        checkDisposed();
//logger.log(Level.DEBUG, "GETPOS: %08x".formatted((fileStream.position() - disk.getOffset())));
        if (fileStream.position() == 0) {
            return 0;
        } else {
            return fileStream.position() - disk.getOffset();
        }
    }

    /** for D88 */
    private int[] chs;
    /** for D88 */
    private int pos;

    /** */
    private void positionInternal(long value) {
        if (value != 0 && disk instanceof D88) {
            // for NOT solid disk (TODO this is ad-hoc because VirtualDisk is for solid disk)
            int sectorOffset = (((int) value / disk.getSectorSize()) * disk.getSectorSize()) + (int) disk.getOffset() - 16;
            chs = disk.search(sectorOffset);
            if (chs == null) {
logger.log(Level.TRACE, "no such sector of offset: argument: %08x, actual: %08x".formatted(sectorOffset, sectorOffset + disk.getOffset() - 16));
                throw new dotnet4j.io.IOException("no such sector of offset: %08x".formatted(sectorOffset));
            }
logger.log(Level.TRACE, "hit sector of offset: chs[%d,%d,%d], argument: %08x, search: %08x".formatted(chs[0], chs[1], chs[2], value, sectorOffset));
            this.pos = (int) value;
        } else {
            fileStream.position(value + disk.getOffset());
        }
    }

    @Override public void position(long value) {
        checkDisposed();
//logger.log(Level.DEBUG, "SETPOS: %08x".formatted(value));
        positionInternal(value);
        atEof = false;
    }

    public BiConsumer<Object, Object[]> writeOccurred;

    @Override public void flush() {
        checkDisposed();
    }

    @Override public int read(byte[] buffer, int offset, int count) {
        checkDisposed();
//new Exception().printStackTrace(System.err);
logger.log(Level.DEBUG, "READ: %08x, %d, %d".formatted(disk instanceof D88 ? pos : fileStream.position(), offset, count));
        if (atEof || fileStream.position() > disk.getLength()) {
            atEof = true;
            throw new dotnet4j.io.IOException("Attempt to read beyond end of file");
        }

        if (fileStream.position() == disk.getLength()) {
            atEof = true;
            return 0;
        }

        if (position() != 0 && disk instanceof D88) {
            // for NOT solid disk (TODO this is ad-hoc because VirtualDisk is for solid disk)
            byte[] sectorData = disk.getSector(chs[0], chs[1], chs[2]).data;
            int secPos = pos % disk.getSectorSize();
            System.arraycopy(sectorData, secPos, buffer, offset, Math.min(count, sectorData.length - secPos)); // TODO multiple sector reading
logger.log(Level.TRACE, "sector[c: %d, h: %d, s: %s] ofs: %08x, len: %08x, sec: %08x%n%s".formatted(chs[0], chs[1], chs[2], offset % disk.getSectorSize(), count, disk.getSectorSize(), StringUtil.getDump(buffer, offset, count)));
        } else {
if (position() == 0) { logger.log(Level.TRACE, "position: 0"); }
            // for solid disk (both jnode and vavi-nio-file-emu)
            StreamUtilities.readExact(fileStream, buffer, offset, count);
        }

        return count;
    }

    @Override public long seek(long offset, SeekOrigin origin) {
        checkDisposed();
        long effectiveOffset = offset;
        if (origin == dotnet4j.io.SeekOrigin.Current) {
            effectiveOffset += fileStream.position();
        } else if (origin == dotnet4j.io.SeekOrigin.End) {
            effectiveOffset += disk.getLength();
        }

        atEof = false;

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        }
        positionInternal(disk.getOffset() + effectiveOffset);
        return fileStream.position() - disk.getOffset();
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

        if (atEof || fileStream.position() + count > disk.getLength()) {
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
