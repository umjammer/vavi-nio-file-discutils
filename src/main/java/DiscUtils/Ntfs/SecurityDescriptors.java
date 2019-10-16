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

package DiscUtils.Ntfs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.AccessControlSections;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.Stream;
import moe.yo3explorer.dotnetio4j.compat.RawSecurityDescriptor;


public final class SecurityDescriptors implements IDiagnosticTraceable {
    // File consists of pairs of duplicate blocks (one after the other), providing
    // redundancy.  When a pair is full, the next pair is used.
    private static final int BlockSize = 0x40000;

    private final File _file;

    private final IndexView<DiscUtils.Ntfs.SecurityDescriptors.HashIndexKey, HashIndexData> _hashIndex;

    private final IndexView<DiscUtils.Ntfs.SecurityDescriptors.IdIndexKey, DiscUtils.Ntfs.SecurityDescriptors.IdIndexData> _idIndex;

    private int _nextId;

    private long _nextSpace;

    public SecurityDescriptors(File file) {
        _file = file;
        _hashIndex = new IndexView<>(file.getIndex("$SDH"));
        _idIndex = new IndexView<>(file.getIndex("$SII"));
        for (Map.Entry<DiscUtils.Ntfs.SecurityDescriptors.IdIndexKey, DiscUtils.Ntfs.SecurityDescriptors.IdIndexData> entry : _idIndex
                .getEntries()
                .entrySet()) {
            if (entry.getKey().Id > _nextId) {
                _nextId = entry.getKey().Id;
            }

            long end = entry.getValue().SdsOffset + entry.getValue().SdsLength;
            if (end > _nextSpace) {
                _nextSpace = end;
            }

        }
        if (_nextId == 0) {
            _nextId = 256;
        } else {
            _nextId++;
        }
        _nextSpace = MathUtilities.roundUp(_nextSpace, 16);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "SECURITY DESCRIPTORS");
        try (Stream s = _file.openStream(AttributeType.Data, "$SDS", FileAccess.Read)) {
            byte[] buffer = StreamUtilities.readExact(s, (int) s.getLength());
            for (Map.Entry<DiscUtils.Ntfs.SecurityDescriptors.IdIndexKey, DiscUtils.Ntfs.SecurityDescriptors.IdIndexData> entry : _idIndex
                    .getEntries()
                    .entrySet()) {
                int pos = (int) entry.getValue().SdsOffset;
                SecurityDescriptorRecord rec = new SecurityDescriptorRecord();
                if (!rec.read(buffer, pos)) {
                    break;
                }

                String secDescStr = "--unknown--";
                if (rec.SecurityDescriptor[0] != 0) {
                    RawSecurityDescriptor sd = new RawSecurityDescriptor(rec.SecurityDescriptor, 0);
                    secDescStr = sd.getSddlForm(AccessControlSections.All);
                }

                writer.println(indent + "  SECURITY DESCRIPTOR RECORD");
                writer.println(indent + "           Hash: " + rec.Hash);
                writer.println(indent + "             Id: " + rec.Id);
                writer.println(indent + "    File Offset: " + rec.OffsetInFile);
                writer.println(indent + "           Size: " + rec.EntrySize);
                writer.println(indent + "          Value: " + secDescStr);
            }
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    public static SecurityDescriptors initialize(File file) {
        file.createIndex("$SDH", AttributeType.__dummyEnum__0, AttributeCollationRule.SecurityHash);
        file.createIndex("$SII", AttributeType.__dummyEnum__0, AttributeCollationRule.UnsignedLong);
        file.createStream(AttributeType.Data, "$SDS");
        return new SecurityDescriptors(file);
    }

    public RawSecurityDescriptor getDescriptorById(int id) {
        // Search to see if this is a known descriptor
        // Write the new descriptor to the end of the existing descriptors
        // If we'd overflow into our duplicate block, skip over it to the
        // start of the next block
        // Make the next descriptor land at the end of this one
        // Update the indexes
        IdIndexData[] data = new IdIndexData[1];
        if (_idIndex.tryGetValue(new DiscUtils.Ntfs.SecurityDescriptors.IdIndexKey(id), data)) {
            return readDescriptor(data[0]).getDescriptor();
        }

        return null;
    }

    public int addDescriptor(RawSecurityDescriptor newDescriptor) {
        SecurityDescriptor newDescObj = new SecurityDescriptor(newDescriptor);
        int newHash = newDescObj.calcHash();
        byte[] newByteForm = new byte[(int) newDescObj.getSize()];
        newDescObj.writeTo(newByteForm, 0);
        for (Map.Entry<DiscUtils.Ntfs.SecurityDescriptors.HashIndexKey, HashIndexData> entry : _hashIndex
                .findAll(new HashFinder(newHash))
                .entrySet()) {
            SecurityDescriptor stored = readDescriptor(entry.getValue());
            byte[] storedByteForm = new byte[(int) stored.getSize()];
            stored.writeTo(storedByteForm, 0);
            if (Utilities.areEqual(newByteForm, storedByteForm)) {
                return entry.getValue().Id;
            }
        }
        long offset = _nextSpace;
        SecurityDescriptorRecord record = new SecurityDescriptorRecord();
        record.SecurityDescriptor = newByteForm;
        record.Hash = newHash;
        record.Id = _nextId;
        if ((offset + record.getSize()) / BlockSize % 2 == 1) {
            _nextSpace = MathUtilities.roundUp(offset, BlockSize * 2);
            offset = _nextSpace;
        }

        record.OffsetInFile = offset;
        byte[] buffer = new byte[(int) record.getSize()];
        record.writeTo(buffer, 0);
        try (Stream s = _file.openStream(AttributeType.Data, "$SDS", FileAccess.ReadWrite)) {
            s.setPosition(_nextSpace);
            s.write(buffer, 0, buffer.length);
            s.setPosition(BlockSize + _nextSpace);
            s.write(buffer, 0, buffer.length);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
        _nextSpace = MathUtilities.roundUp(_nextSpace + buffer.length, 16);
        _nextId++;
        HashIndexData hashIndexData = new HashIndexData();
        hashIndexData.Hash = record.Hash;
        hashIndexData.Id = record.Id;
        hashIndexData.SdsOffset = record.OffsetInFile;
        hashIndexData.SdsLength = record.EntrySize;
        DiscUtils.Ntfs.SecurityDescriptors.HashIndexKey hashIndexKey = new DiscUtils.Ntfs.SecurityDescriptors.HashIndexKey();
        hashIndexKey.Hash = record.Hash;
        hashIndexKey.Id = record.Id;
        _hashIndex.set___idx(hashIndexKey, hashIndexData);
        DiscUtils.Ntfs.SecurityDescriptors.IdIndexData idIndexData = new DiscUtils.Ntfs.SecurityDescriptors.IdIndexData();
        idIndexData.Hash = record.Hash;
        idIndexData.Id = record.Id;
        idIndexData.SdsOffset = record.OffsetInFile;
        idIndexData.SdsLength = record.EntrySize;
        DiscUtils.Ntfs.SecurityDescriptors.IdIndexKey idIndexKey = new DiscUtils.Ntfs.SecurityDescriptors.IdIndexKey();
        idIndexKey.Id = record.Id;
        _idIndex.set___idx(idIndexKey, idIndexData);
        _file.updateRecordInMft();
        return record.Id;
    }

    private SecurityDescriptor readDescriptor(IndexData data) {
        try (Stream s = _file.openStream(AttributeType.Data, "$SDS", FileAccess.Read)) {
            s.setPosition(data.SdsOffset);
            byte[] buffer = StreamUtilities.readExact(s, data.SdsLength);
            SecurityDescriptorRecord record = new SecurityDescriptorRecord();
            record.read(buffer, 0);
            return new SecurityDescriptor(new RawSecurityDescriptor(record.SecurityDescriptor, 0));
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    public abstract static class IndexData {
        public int Hash;

        public int Id;

        public int SdsLength;

        public long SdsOffset;

        public String toString() {
            try {
                return String.format("[Data-Hash:%x,Id:%d,SdsOffset:%d,SdsLength:%d]", Hash, Id, SdsOffset, SdsLength);
            } catch (RuntimeException __dummyCatchVar0) {
                throw __dummyCatchVar0;
            } catch (Exception __dummyCatchVar0) {
                throw new RuntimeException(__dummyCatchVar0);
            }
        }
    }

    public final static class HashIndexKey implements IByteArraySerializable {
        public int Hash;

        public int Id;

        public long getSize() {
            return 8;
        }

        public int readFrom(byte[] buffer, int offset) {
            Hash = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
            Id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
            return 8;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(Hash, buffer, offset + 0);
            EndianUtilities.writeBytesLittleEndian(Id, buffer, offset + 4);
        }

        public String toString() {
            try {
                return String.format("[Key-Hash:%x,Id:%d]", Hash, Id);
            } catch (RuntimeException __dummyCatchVar1) {
                throw __dummyCatchVar1;
            } catch (Exception __dummyCatchVar1) {
                throw new RuntimeException(__dummyCatchVar1);
            }
        }
    }

    public final static class HashIndexData extends IndexData implements IByteArraySerializable {
        public long getSize() {
            return 0x14;
        }

        public int readFrom(byte[] buffer, int offset) {
            Hash = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00);
            Id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x04);
            SdsOffset = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x08);
            SdsLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x10);
            return 0x14;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(Hash, buffer, offset + 0x00);
            EndianUtilities.writeBytesLittleEndian(Id, buffer, offset + 0x04);
            EndianUtilities.writeBytesLittleEndian(SdsOffset, buffer, offset + 0x08);
            EndianUtilities.writeBytesLittleEndian(SdsLength, buffer, offset + 0x10);
        }
    }

    //System.arraycopy(new byte[] { (byte)'I', 0, (byte)'I', 0 }, 0, buffer, offset + 0x14, 4);

    public final static class IdIndexKey implements IByteArraySerializable {
        public int Id;

        public IdIndexKey() {
        }

        public IdIndexKey(int id) {
            Id = id;
        }

        public long getSize() {
            return 4;
        }

        public int readFrom(byte[] buffer, int offset) {
            Id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
            return 4;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(Id, buffer, offset + 0);
        }

        public String toString() {
            return String.format("[Key-Id:%d]", Id);
        }
    }

    public final static class IdIndexData extends IndexData implements IByteArraySerializable {
        public long getSize() {
            return 0x14;
        }

        public int readFrom(byte[] buffer, int offset) {
            Hash = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00);
            Id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x04);
            SdsOffset = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x08);
            SdsLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x10);
            return 0x14;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(Hash, buffer, offset + 0x00);
            EndianUtilities.writeBytesLittleEndian(Id, buffer, offset + 0x04);
            EndianUtilities.writeBytesLittleEndian(SdsOffset, buffer, offset + 0x08);
            EndianUtilities.writeBytesLittleEndian(SdsLength, buffer, offset + 0x10);
        }
    }

    private static class HashFinder implements Comparable<DiscUtils.Ntfs.SecurityDescriptors.HashIndexKey> {
        private final int _toMatch;

        public HashFinder(int toMatch) {
            _toMatch = toMatch;
        }

        public int compareTo(DiscUtils.Ntfs.SecurityDescriptors.HashIndexKey other) {
            return compareTo(other.Hash);
        }

        public int compareTo(int otherHash) {
            if (_toMatch < otherHash) {
                return -1;
            }

            if (_toMatch > otherHash) {
                return 1;
            }

            return 0;
        }
    }
}
