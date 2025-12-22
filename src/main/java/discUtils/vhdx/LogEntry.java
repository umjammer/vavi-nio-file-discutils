//
// Copyright (c) 2008-2013, Kenneth Bell
// Copyright (c) 2017, Bianco Veigel
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

package discUtils.vhdx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import discUtils.core.internal.Crc32Algorithm;
import discUtils.core.internal.Crc32LittleEndian;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Range;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


public final class LogEntry {

    public static final int LogSectorSize = (int) (4 * Sizes.OneKiB);

    private final List<Descriptor> descriptors;

    private final LogEntryHeader header;

    private LogEntry(long position, LogEntryHeader header, List<Descriptor> descriptors) {
        this.position = position;
        this.header = header;
        this.descriptors = descriptors;
    }

    public long getFlushedFileOffset() {
        return header.flushedFileOffset;
    }

    public boolean isEmpty() {
        return descriptors.isEmpty();
    }

    public long getLastFileOffset() {
        return header.lastFileOffset;
    }

    public UUID getLogGuid() {
        return header.logGuid;
    }

    public List<Range> getModifiedExtents() {
        return descriptors.stream().map(d -> new Range(d.fileOffset, d.getFileLength())).collect(Collectors.toList());
    }

    private final long position;

    public long getPosition() {
        return position;
    }

    public long getSequenceNumber() {
        return header.sequenceNumber;
    }

    public int getTail() {
        return header.tail;
    }

    public void replay(Stream target) {
        if (isEmpty())
            return;

        for (Descriptor descriptor : descriptors) {
            descriptor.writeData(target);
        }
    }

    /**
     * @param entry {@cs out}
     */
    public static boolean tryRead(Stream logStream, LogEntry[] entry) {
        long position = logStream.position();

        byte[] sectorBuffer = new byte[LogSectorSize];
        if (StreamUtilities.readMaximum(logStream, sectorBuffer, 0, sectorBuffer.length) != sectorBuffer.length) {
            entry[0] = null;
            return false;
        }

        int sig = ByteUtil.readLeInt(sectorBuffer, 0);
        if (sig != LogEntryHeader.LogEntrySignature) {
            entry[0] = null;
            return false;
        }

        LogEntryHeader header = new LogEntryHeader();
        header.readFrom(sectorBuffer, 0);

        if (!header.isValid() || header.entryLength > logStream.getLength()) {
            entry[0] = null;
            return false;
        }

        byte[] logEntryBuffer = new byte[header.entryLength];
        System.arraycopy(sectorBuffer, 0, logEntryBuffer, 0, LogSectorSize);

        StreamUtilities.readExact(logStream, logEntryBuffer, LogSectorSize, logEntryBuffer.length - LogSectorSize);

        ByteUtil.writeLeInt(0, logEntryBuffer, 4);
        if (header.checksum != Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, logEntryBuffer, 0, header.entryLength)) {
            entry[0] = null;
            return false;
        }

        int dataPos = MathUtilities.roundUp(header.descriptorCount * 32 + 64, LogSectorSize);

        List<Descriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < header.descriptorCount; ++i) {
            int offset = i * 32 + 64;
            Descriptor descriptor;

            int descriptorSig = ByteUtil.readLeInt(logEntryBuffer, offset);
            switch (descriptorSig) {
            case Descriptor.ZeroDescriptorSignature:
                descriptor = new ZeroDescriptor();
                break;
            case Descriptor.DataDescriptorSignature:
                descriptor = new DataDescriptor(logEntryBuffer, dataPos);
                dataPos += LogSectorSize;
                break;
            default:
                entry[0] = null;
                return false;
            }

            descriptor.readFrom(logEntryBuffer, offset);
            if (!descriptor.isValid(header.sequenceNumber)) {
                entry[0] = null;
                return false;
            }

            descriptors.add(descriptor);
        }

        entry[0] = new LogEntry(position, header, descriptors);
        return true;
    }

    private abstract static class Descriptor implements IByteArraySerializable {

        public static final int ZeroDescriptorSignature = 0x6F72657A;

        public static final int DataDescriptorSignature = 0x63736564;

        public static final int DataSectorSignature = 0x61746164;

        public long fileOffset;

        public long sequenceNumber;

        public abstract long getFileLength();

        @Override public int size() {
            return 32;
        }

        @Override public abstract int readFrom(byte[] buffer, int offset);

        @Override public abstract void writeTo(byte[] buffer, int offset);

        public abstract boolean isValid(long sequenceNumber);

        public abstract void writeData(Stream target);
    }

    private final static class ZeroDescriptor extends Descriptor {

        public long zeroLength;

        @Override public long getFileLength() {
            return zeroLength;
        }

        @Override public int readFrom(byte[] buffer, int offset) {
            zeroLength = ByteUtil.readLeLong(buffer, offset + 8);
            fileOffset = ByteUtil.readLeLong(buffer, offset + 16);
            sequenceNumber = ByteUtil.readLeLong(buffer, offset + 24);

            return 32;
        }

        @Override public void writeTo(byte[] buffer, int offset) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean isValid(long sequenceNumber) {
            return this.sequenceNumber == sequenceNumber;
        }

        @Override public void writeData(Stream target) {
            target.seek(fileOffset, SeekOrigin.Begin);
            byte[] zeroBuffer = new byte[(int) (4 * Sizes.OneKiB)];
            long total = zeroLength;
            while (total > 0) {
                int count = zeroBuffer.length;
                if (total < count)
                    count = (int) total;
                target.write(zeroBuffer, 0, count);
                total -= count;
            }
        }
    }

    private final static class DataDescriptor extends Descriptor {

        private final byte[] data;

        private final int offset;

        public long leadingBytes;

        public int trailingBytes;

        public int dataSignature;

        public DataDescriptor(byte[] data, int offset) {
            this.data = data;
            this.offset = offset;
        }

        @Override public long getFileLength() {
            return LogSectorSize;
        }

        @Override public int readFrom(byte[] buffer, int offset) {
            trailingBytes = ByteUtil.readLeInt(buffer, offset + 4);
            leadingBytes = ByteUtil.readLeLong(buffer, offset + 8);
            fileOffset = ByteUtil.readLeLong(buffer, offset + 16);
            sequenceNumber = ByteUtil.readLeLong(buffer, offset + 24);

            dataSignature = ByteUtil.readLeInt(data, this.offset);

            return 32;
        }

        @Override public void writeTo(byte[] buffer, int offset) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean isValid(long sequenceNumber) {
            return this.sequenceNumber == sequenceNumber && offset + LogSectorSize <= data.length &&
                   (ByteUtil.readLeInt(data, offset + LogSectorSize - 4) & 0xFFFF_FFFFL) == (sequenceNumber &
                           0xFFFF_FFFFL) &&
                   (ByteUtil.readLeInt(data, offset + 4) &
                           0xFFFF_FFFFL) == ((sequenceNumber >>> 32) & 0xFFFF_FFFFL) &&
                   dataSignature == DataSectorSignature;
        }

        @Override public void writeData(Stream target) {
            target.seek(fileOffset, SeekOrigin.Begin);
            byte[] leading = new byte[8];
            ByteUtil.writeLeLong(leadingBytes, leading, 0);
            byte[] trailing = new byte[4];
            ByteUtil.writeLeInt(trailingBytes, trailing, 0);

            target.write(leading, 0, leading.length);
            target.write(data, offset + 8, 4084);
            target.write(trailing, 0, trailing.length);
        }
    }
}
