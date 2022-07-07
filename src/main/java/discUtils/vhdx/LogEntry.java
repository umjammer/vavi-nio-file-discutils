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
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Range;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public final class LogEntry {
    public static final int LogSectorSize = (int) (4 * Sizes.OneKiB);

    private final List<Descriptor> _descriptors;

    private final LogEntryHeader _header;

    private LogEntry(long position, LogEntryHeader header, List<Descriptor> descriptors) {
        _position = position;
        _header = header;
        _descriptors = descriptors;
    }

    public long getFlushedFileOffset() {
        return _header.FlushedFileOffset;
    }

    public boolean isEmpty() {
        return _descriptors.size() == 0;
    }

    public long getLastFileOffset() {
        return _header.LastFileOffset;
    }

    public UUID getLogGuid() {
        return _header.LogGuid;
    }

    public List<Range> getModifiedExtents() {
        return _descriptors.stream().map(d -> new Range(d.FileOffset, d.getFileLength())).collect(Collectors.toList());
    }

    private long _position;

    public long getPosition() {
        return _position;
    }

    public long getSequenceNumber() {
        return _header.SequenceNumber;
    }

    public int getTail() {
        return _header.Tail;
    }

    public void replay(Stream target) {
        if (isEmpty())
            return;

        for (Descriptor descriptor : _descriptors) {
            descriptor.writeData(target);
        }
    }

    /**
     * @param entry {@cs out}
     */
    public static boolean tryRead(Stream logStream, LogEntry[] entry) {
        long position = logStream.getPosition();

        byte[] sectorBuffer = new byte[LogSectorSize];
        if (StreamUtilities.readMaximum(logStream, sectorBuffer, 0, sectorBuffer.length) != sectorBuffer.length) {
            entry[0] = null;
            return false;
        }

        int sig = EndianUtilities.toUInt32LittleEndian(sectorBuffer, 0);
        if (sig != LogEntryHeader.LogEntrySignature) {
            entry[0] = null;
            return false;
        }

        LogEntryHeader header = new LogEntryHeader();
        header.readFrom(sectorBuffer, 0);

        if (!header.isValid() || header.EntryLength > logStream.getLength()) {
            entry[0] = null;
            return false;
        }

        byte[] logEntryBuffer = new byte[header.EntryLength];
        System.arraycopy(sectorBuffer, 0, logEntryBuffer, 0, LogSectorSize);

        StreamUtilities.readExact(logStream, logEntryBuffer, LogSectorSize, logEntryBuffer.length - LogSectorSize);

        EndianUtilities.writeBytesLittleEndian(0, logEntryBuffer, 4);
        if (header.Checksum != Crc32LittleEndian.compute(Crc32Algorithm.Castagnoli, logEntryBuffer, 0, header.EntryLength)) {
            entry[0] = null;
            return false;
        }

        int dataPos = MathUtilities.roundUp(header.DescriptorCount * 32 + 64, LogSectorSize);

        List<Descriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < header.DescriptorCount; ++i) {
            int offset = i * 32 + 64;
            Descriptor descriptor;

            int descriptorSig = EndianUtilities.toUInt32LittleEndian(logEntryBuffer, offset);
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
            if (!descriptor.isValid(header.SequenceNumber)) {
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

        public long FileOffset;

        public long SequenceNumber;

        public abstract long getFileLength();

        public int size() {
            return 32;
        }

        public abstract int readFrom(byte[] buffer, int offset);

        public abstract void writeTo(byte[] buffer, int offset);

        public abstract boolean isValid(long sequenceNumber);

        public abstract void writeData(Stream target);
    }

    private final static class ZeroDescriptor extends Descriptor {
        public long ZeroLength;

        public long getFileLength() {
            return ZeroLength;
        }

        public int readFrom(byte[] buffer, int offset) {
            ZeroLength = EndianUtilities.toUInt64LittleEndian(buffer, offset + 8);
            FileOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 16);
            SequenceNumber = EndianUtilities.toUInt64LittleEndian(buffer, offset + 24);

            return 32;
        }

        public void writeTo(byte[] buffer, int offset) {
            throw new UnsupportedOperationException();
        }

        public boolean isValid(long sequenceNumber) {
            return SequenceNumber == sequenceNumber;
        }

        public void writeData(Stream target) {
            target.seek(FileOffset, SeekOrigin.Begin);
            byte[] zeroBuffer = new byte[(int) (4 * Sizes.OneKiB)];
            long total = ZeroLength;
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
        private final byte[] _data;

        private final int _offset;

        public long LeadingBytes;

        public int TrailingBytes;

        public int DataSignature;

        public DataDescriptor(byte[] data, int offset) {
            _data = data;
            _offset = offset;
        }

        public long getFileLength() {
            return LogSectorSize;
        }

        public int readFrom(byte[] buffer, int offset) {
            TrailingBytes = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
            LeadingBytes = EndianUtilities.toUInt64LittleEndian(buffer, offset + 8);
            FileOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 16);
            SequenceNumber = EndianUtilities.toUInt64LittleEndian(buffer, offset + 24);

            DataSignature = EndianUtilities.toUInt32LittleEndian(_data, _offset);

            return 32;
        }

        public void writeTo(byte[] buffer, int offset) {
            throw new UnsupportedOperationException();
        }

        public boolean isValid(long sequenceNumber) {
            return SequenceNumber == sequenceNumber && _offset + LogSectorSize <= _data.length &&
                   (EndianUtilities.toUInt32LittleEndian(_data, _offset + LogSectorSize - 4) & 0xFFFFFFFFL) == (sequenceNumber &
                           0xFFFFFFFFL) &&
                   (EndianUtilities.toUInt32LittleEndian(_data, _offset + 4) &
                           0xFFFFFFFFL) == ((sequenceNumber >>> 32) & 0xFFFFFFFFL) &&
                   DataSignature == DataSectorSignature;
        }

        public void writeData(Stream target) {
            target.seek(FileOffset, SeekOrigin.Begin);
            byte[] leading = new byte[8];
            EndianUtilities.writeBytesLittleEndian(LeadingBytes, leading, 0);
            byte[] trailing = new byte[4];
            EndianUtilities.writeBytesLittleEndian(TrailingBytes, trailing, 0);

            target.write(leading, 0, leading.length);
            target.write(_data, _offset + 8, 4084);
            target.write(trailing, 0, trailing.length);
        }
    }
}
