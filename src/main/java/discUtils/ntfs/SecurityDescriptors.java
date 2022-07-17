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

package discUtils.ntfs;

import java.io.IOException;
import java.io.PrintWriter;

import discUtils.core.IDiagnosticTraceable;
import discUtils.core.internal.Utilities;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.util.compat.Tuple;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;
import dotnet4j.security.accessControl.AccessControlSections;
import dotnet4j.security.accessControl.RawSecurityDescriptor;


final class SecurityDescriptors implements IDiagnosticTraceable {
    // File consists of pairs of duplicate blocks (one after the other), providing
    // redundancy. When a pair is full, the next pair is used.
    private static final int BlockSize = 0x40000;

    private final File _file;

    private final IndexView<discUtils.ntfs.SecurityDescriptors.HashIndexKey, HashIndexData> _hashIndex;

    private final IndexView<discUtils.ntfs.SecurityDescriptors.IdIndexKey, discUtils.ntfs.SecurityDescriptors.IdIndexData> _idIndex;

    private int _nextId;

    private long _nextSpace;

    public SecurityDescriptors(File file) {
        _file = file;
        _hashIndex = new IndexView<>(discUtils.ntfs.SecurityDescriptors.HashIndexKey.class,
                                     HashIndexData.class,
                                     file.getIndex("$SDH"));
        _idIndex = new IndexView<>(discUtils.ntfs.SecurityDescriptors.IdIndexKey.class,
                                   discUtils.ntfs.SecurityDescriptors.IdIndexData.class,
                                   file.getIndex("$SII"));
        for (Tuple<discUtils.ntfs.SecurityDescriptors.IdIndexKey, discUtils.ntfs.SecurityDescriptors.IdIndexData> entry : _idIndex
                .getEntries()) {
            if (entry.getKey()._id > _nextId) {
                _nextId = entry.getKey()._id;
            }

            long end = entry.getValue()._sdsOffset + entry.getValue()._sdsLength;
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
            for (Tuple<discUtils.ntfs.SecurityDescriptors.IdIndexKey, discUtils.ntfs.SecurityDescriptors.IdIndexData> entry : _idIndex
                    .getEntries()) {
                int pos = (int) entry.getValue()._sdsOffset;
                SecurityDescriptorRecord rec = new SecurityDescriptorRecord();
                if (!rec.read(buffer, pos)) {
                    break;
                }

                String secDescStr = "--unknown--";
                if (rec._securityDescriptor[0] != 0) {
                    RawSecurityDescriptor sd = new RawSecurityDescriptor(rec._securityDescriptor, 0);
                    secDescStr = sd.getSddlForm(AccessControlSections.All);
                }

                writer.println(indent + "  SECURITY DESCRIPTOR RECORD");
                writer.println(indent + "           Hash: " + rec._hash);
                writer.println(indent + "             Id: " + rec._id);
                writer.println(indent + "    File Offset: " + rec._offsetInFile);
                writer.println(indent + "           Size: " + rec._entrySize);
                writer.println(indent + "          Value: " + secDescStr);
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public static SecurityDescriptors initialize(File file) {
        file.createIndex("$SDH", AttributeType.None, AttributeCollationRule.SecurityHash);
        file.createIndex("$SII", AttributeType.None, AttributeCollationRule.UnsignedLong);
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
        if (_idIndex.tryGetValue(new discUtils.ntfs.SecurityDescriptors.IdIndexKey(id), data)) {
            return readDescriptor(data[0]).getDescriptor();
        }

        return null;
    }

    public int addDescriptor(RawSecurityDescriptor newDescriptor) {
        SecurityDescriptor newDescObj = new SecurityDescriptor(newDescriptor);
        int newHash = newDescObj.calcHash();
        byte[] newByteForm = new byte[newDescObj.size()];
        newDescObj.writeTo(newByteForm, 0);
        for (Tuple<discUtils.ntfs.SecurityDescriptors.HashIndexKey, HashIndexData> entry : _hashIndex
                .findAll(new HashFinder(newHash))) {
            SecurityDescriptor stored = readDescriptor(entry.getValue());
            byte[] storedByteForm = new byte[stored.size()];
            stored.writeTo(storedByteForm, 0);
            if (Utilities.areEqual(newByteForm, storedByteForm)) {
                return entry.getValue()._id;
            }
        }
        long offset = _nextSpace;
        SecurityDescriptorRecord record = new SecurityDescriptorRecord();
        record._securityDescriptor = newByteForm;
        record._hash = newHash;
        record._id = _nextId;
        if ((offset + record.size()) / BlockSize % 2 == 1) {
            _nextSpace = MathUtilities.roundUp(offset, BlockSize * 2);
            offset = _nextSpace;
        }

        record._offsetInFile = offset;
        byte[] buffer = new byte[record.size()];
        record.writeTo(buffer, 0);
        try (Stream s = _file.openStream(AttributeType.Data, "$SDS", FileAccess.ReadWrite)) {
            s.setPosition(_nextSpace);
            s.write(buffer, 0, buffer.length);
            s.setPosition(BlockSize + _nextSpace);
            s.write(buffer, 0, buffer.length);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        _nextSpace = MathUtilities.roundUp(_nextSpace + buffer.length, 16);
        _nextId++;
        HashIndexData hashIndexData = new HashIndexData();
        hashIndexData._hash = record._hash;
        hashIndexData._id = record._id;
        hashIndexData._sdsOffset = record._offsetInFile;
        hashIndexData._sdsLength = record._entrySize;
        discUtils.ntfs.SecurityDescriptors.HashIndexKey hashIndexKey = new discUtils.ntfs.SecurityDescriptors.HashIndexKey();
        hashIndexKey._hash = record._hash;
        hashIndexKey._id = record._id;
        _hashIndex.put(hashIndexKey, hashIndexData);
        discUtils.ntfs.SecurityDescriptors.IdIndexData idIndexData = new discUtils.ntfs.SecurityDescriptors.IdIndexData();
        idIndexData._hash = record._hash;
        idIndexData._id = record._id;
        idIndexData._sdsOffset = record._offsetInFile;
        idIndexData._sdsLength = record._entrySize;
        discUtils.ntfs.SecurityDescriptors.IdIndexKey idIndexKey = new discUtils.ntfs.SecurityDescriptors.IdIndexKey();
        idIndexKey._id = record._id;
        _idIndex.put(idIndexKey, idIndexData);
        _file.updateRecordInMft();
        return record._id;
    }

    private SecurityDescriptor readDescriptor(IndexData data) {
        try (Stream s = _file.openStream(AttributeType.Data, "$SDS", FileAccess.Read)) {
            s.setPosition(data._sdsOffset);
            byte[] buffer = StreamUtilities.readExact(s, data._sdsLength);
            SecurityDescriptorRecord record = new SecurityDescriptorRecord();
            record.read(buffer, 0);
            return new SecurityDescriptor(new RawSecurityDescriptor(record._securityDescriptor, 0));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    abstract static class IndexData implements IByteArraySerializable {
        public int _hash;

        public int _id;

        public int _sdsLength;

        public long _sdsOffset;

        public String toString() {
            return String.format("[Data-Hash:%x,Id:%d,SdsOffset:%d,SdsLength:%d]", _hash, _id, _sdsOffset, _sdsLength);
        }
    }

    final static class HashIndexKey implements IByteArraySerializable {
        public int _hash;

        public int _id;

        public int size() {
            return 8;
        }

        public int readFrom(byte[] buffer, int offset) {
            _hash = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
            _id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
            return 8;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(_hash, buffer, offset + 0);
            EndianUtilities.writeBytesLittleEndian(_id, buffer, offset + 4);
        }

        public String toString() {
            return String.format("[Key-Hash:%x,Id:%d]", _hash, _id);
        }
    }

    final static class HashIndexData extends IndexData implements IByteArraySerializable {
        public int size() {
            return 0x14;
        }

        public int readFrom(byte[] buffer, int offset) {
            _hash = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00);
            _id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x04);
            _sdsOffset = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x08);
            _sdsLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x10);
            return 0x14;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(_hash, buffer, offset + 0x00);
            EndianUtilities.writeBytesLittleEndian(_id, buffer, offset + 0x04);
            EndianUtilities.writeBytesLittleEndian(_sdsOffset, buffer, offset + 0x08);
            EndianUtilities.writeBytesLittleEndian(_sdsLength, buffer, offset + 0x10);
//            System.arraycopy(new byte[] { (byte) 'I', 0, (byte) 'I', 0 }, 0, buffer, offset + 0x14, 4);
        }
    }

    final static class IdIndexKey implements IByteArraySerializable {
        public int _id;

        public IdIndexKey() {
        }

        public IdIndexKey(int id) {
            _id = id;
        }

        public int size() {
            return 4;
        }

        public int readFrom(byte[] buffer, int offset) {
            _id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
            return 4;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(_id, buffer, offset + 0);
        }

        public String toString() {
            return String.format("[Key-Id:%d]", _id);
        }
    }

    final static class IdIndexData extends IndexData implements IByteArraySerializable {
        public int size() {
            return 0x14;
        }

        public int readFrom(byte[] buffer, int offset) {
            _hash = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00);
            _id = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x04);
            _sdsOffset = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x08);
            _sdsLength = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x10);
            return 0x14;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(_hash, buffer, offset + 0x00);
            EndianUtilities.writeBytesLittleEndian(_id, buffer, offset + 0x04);
            EndianUtilities.writeBytesLittleEndian(_sdsOffset, buffer, offset + 0x08);
            EndianUtilities.writeBytesLittleEndian(_sdsLength, buffer, offset + 0x10);
        }
    }

    private static class HashFinder implements Comparable<discUtils.ntfs.SecurityDescriptors.HashIndexKey> {
        private final int _toMatch;

        public HashFinder(int toMatch) {
            _toMatch = toMatch;
        }

        public int compareTo(discUtils.ntfs.SecurityDescriptors.HashIndexKey other) {
            return compareTo(other._hash);
        }

        public int compareTo(int otherHash) {
            return Integer.compare(_toMatch, otherHash);

        }
    }
}
