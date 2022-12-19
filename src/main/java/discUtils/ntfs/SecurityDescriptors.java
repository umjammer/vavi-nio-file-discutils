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
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;
import dotnet4j.security.accessControl.AccessControlSections;
import dotnet4j.security.accessControl.RawSecurityDescriptor;
import dotnet4j.util.compat.Tuple;
import vavi.util.ByteUtil;


final class SecurityDescriptors implements IDiagnosticTraceable {

    // File consists of pairs of duplicate blocks (one after the other), providing
    // redundancy. When a pair is full, the next pair is used.
    private static final int BlockSize = 0x40000;

    private final File file;

    private final IndexView<discUtils.ntfs.SecurityDescriptors.HashIndexKey, HashIndexData> hashIndex;

    private final IndexView<discUtils.ntfs.SecurityDescriptors.IdIndexKey, discUtils.ntfs.SecurityDescriptors.IdIndexData> idIndex;

    private int nextId;

    private long nextSpace;

    public SecurityDescriptors(File file) {
        this.file = file;
        hashIndex = new IndexView<>(discUtils.ntfs.SecurityDescriptors.HashIndexKey.class,
                                     HashIndexData.class,
                                     file.getIndex("$SDH"));
        idIndex = new IndexView<>(discUtils.ntfs.SecurityDescriptors.IdIndexKey.class,
                                   discUtils.ntfs.SecurityDescriptors.IdIndexData.class,
                                   file.getIndex("$SII"));
        for (Tuple<discUtils.ntfs.SecurityDescriptors.IdIndexKey, discUtils.ntfs.SecurityDescriptors.IdIndexData> entry : idIndex
                .getEntries()) {
            if (entry.getKey().id > nextId) {
                nextId = entry.getKey().id;
            }

            long end = entry.getValue().sdsOffset + entry.getValue().sdsLength;
            if (end > nextSpace) {
                nextSpace = end;
            }

        }
        if (nextId == 0) {
            nextId = 256;
        } else {
            nextId++;
        }
        nextSpace = MathUtilities.roundUp(nextSpace, 16);
    }

    @Override public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "SECURITY DESCRIPTORS");
        try (Stream s = file.openStream(AttributeType.Data, "$SDS", FileAccess.Read)) {
            byte[] buffer = StreamUtilities.readExact(s, (int) s.getLength());
            for (Tuple<discUtils.ntfs.SecurityDescriptors.IdIndexKey, discUtils.ntfs.SecurityDescriptors.IdIndexData> entry : idIndex
                    .getEntries()) {
                int pos = (int) entry.getValue().sdsOffset;
                SecurityDescriptorRecord rec = new SecurityDescriptorRecord();
                if (!rec.read(buffer, pos)) {
                    break;
                }

                String secDescStr = "--unknown--";
                if (rec.securityDescriptor[0] != 0) {
                    RawSecurityDescriptor sd = new RawSecurityDescriptor(rec.securityDescriptor, 0);
                    secDescStr = sd.getSddlForm(AccessControlSections.All);
                }

                writer.println(indent + "  SECURITY DESCRIPTOR RECORD");
                writer.println(indent + "           Hash: " + rec.hash);
                writer.println(indent + "             Id: " + rec.id);
                writer.println(indent + "    File Offset: " + rec.offsetInFile);
                writer.println(indent + "           Size: " + rec.entrySize);
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
        if (idIndex.tryGetValue(new discUtils.ntfs.SecurityDescriptors.IdIndexKey(id), data)) {
            return readDescriptor(data[0]).getDescriptor();
        }

        return null;
    }

    public int addDescriptor(RawSecurityDescriptor newDescriptor) {
        SecurityDescriptor newDescObj = new SecurityDescriptor(newDescriptor);
        int newHash = newDescObj.calcHash();
        byte[] newByteForm = new byte[newDescObj.size()];
        newDescObj.writeTo(newByteForm, 0);
        for (Tuple<discUtils.ntfs.SecurityDescriptors.HashIndexKey, HashIndexData> entry : hashIndex
                .findAll(new HashFinder(newHash))) {
            SecurityDescriptor stored = readDescriptor(entry.getValue());
            byte[] storedByteForm = new byte[stored.size()];
            stored.writeTo(storedByteForm, 0);
            if (Utilities.areEqual(newByteForm, storedByteForm)) {
                return entry.getValue().id;
            }
        }
        long offset = nextSpace;
        SecurityDescriptorRecord record = new SecurityDescriptorRecord();
        record.securityDescriptor = newByteForm;
        record.hash = newHash;
        record.id = nextId;
        if ((offset + record.size()) / BlockSize % 2 == 1) {
            nextSpace = MathUtilities.roundUp(offset, BlockSize * 2);
            offset = nextSpace;
        }

        record.offsetInFile = offset;
        byte[] buffer = new byte[record.size()];
        record.writeTo(buffer, 0);
        try (Stream s = file.openStream(AttributeType.Data, "$SDS", FileAccess.ReadWrite)) {
            s.position(nextSpace);
            s.write(buffer, 0, buffer.length);
            s.position(BlockSize + nextSpace);
            s.write(buffer, 0, buffer.length);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        nextSpace = MathUtilities.roundUp(nextSpace + buffer.length, 16);
        nextId++;
        HashIndexData hashIndexData = new HashIndexData();
        hashIndexData.hash = record.hash;
        hashIndexData.id = record.id;
        hashIndexData.sdsOffset = record.offsetInFile;
        hashIndexData.sdsLength = record.entrySize;
        discUtils.ntfs.SecurityDescriptors.HashIndexKey hashIndexKey = new discUtils.ntfs.SecurityDescriptors.HashIndexKey();
        hashIndexKey.hash = record.hash;
        hashIndexKey.id = record.id;
        hashIndex.put(hashIndexKey, hashIndexData);
        discUtils.ntfs.SecurityDescriptors.IdIndexData idIndexData = new discUtils.ntfs.SecurityDescriptors.IdIndexData();
        idIndexData.hash = record.hash;
        idIndexData.id = record.id;
        idIndexData.sdsOffset = record.offsetInFile;
        idIndexData.sdsLength = record.entrySize;
        discUtils.ntfs.SecurityDescriptors.IdIndexKey idIndexKey = new discUtils.ntfs.SecurityDescriptors.IdIndexKey();
        idIndexKey.id = record.id;
        idIndex.put(idIndexKey, idIndexData);
        file.updateRecordInMft();
        return record.id;
    }

    private SecurityDescriptor readDescriptor(IndexData data) {
        try (Stream s = file.openStream(AttributeType.Data, "$SDS", FileAccess.Read)) {
            s.position(data.sdsOffset);
            byte[] buffer = StreamUtilities.readExact(s, data.sdsLength);
            SecurityDescriptorRecord record = new SecurityDescriptorRecord();
            record.read(buffer, 0);
            return new SecurityDescriptor(new RawSecurityDescriptor(record.securityDescriptor, 0));
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    abstract static class IndexData implements IByteArraySerializable {

        public int hash;

        public int id;

        public int sdsLength;

        public long sdsOffset;

        @Override public String toString() {
            return String.format("[Data-Hash:%x,Id:%d,SdsOffset:%d,SdsLength:%d]", hash, id, sdsOffset, sdsLength);
        }
    }

    final static class HashIndexKey implements IByteArraySerializable {

        public int hash;

        public int id;

        @Override public int size() {
            return 8;
        }

        @Override public int readFrom(byte[] buffer, int offset) {
            hash = ByteUtil.readLeInt(buffer, offset + 0);
            id = ByteUtil.readLeInt(buffer, offset + 4);
            return 8;
        }

        @Override public void writeTo(byte[] buffer, int offset) {
            ByteUtil.writeLeInt(hash, buffer, offset + 0);
            ByteUtil.writeLeInt(id, buffer, offset + 4);
        }

        @Override public String toString() {
            return String.format("[Key-Hash:%x,Id:%d]", hash, id);
        }
    }

    final static class HashIndexData extends IndexData implements IByteArraySerializable {

        @Override public int size() {
            return 0x14;
        }

        @Override public int readFrom(byte[] buffer, int offset) {
            hash = ByteUtil.readLeInt(buffer, offset + 0x00);
            id = ByteUtil.readLeInt(buffer, offset + 0x04);
            sdsOffset = ByteUtil.readLeLong(buffer, offset + 0x08);
            sdsLength = ByteUtil.readLeInt(buffer, offset + 0x10);
            return 0x14;
        }

        @Override public void writeTo(byte[] buffer, int offset) {
            ByteUtil.writeLeInt(hash, buffer, offset + 0x00);
            ByteUtil.writeLeInt(id, buffer, offset + 0x04);
            ByteUtil.writeLeLong(sdsOffset, buffer, offset + 0x08);
            ByteUtil.writeLeInt(sdsLength, buffer, offset + 0x10);
//            System.arraycopy(new byte[] { (byte) 'I', 0, (byte) 'I', 0 }, 0, buffer, offset + 0x14, 4);
        }
    }

    final static class IdIndexKey implements IByteArraySerializable {

        public int id;

        public IdIndexKey() {
        }

        public IdIndexKey(int id) {
            this.id = id;
        }

        @Override public int size() {
            return 4;
        }

        @Override public int readFrom(byte[] buffer, int offset) {
            id = ByteUtil.readLeInt(buffer, offset + 0);
            return 4;
        }

        @Override public void writeTo(byte[] buffer, int offset) {
            ByteUtil.writeLeInt(id, buffer, offset + 0);
        }

        @Override public String toString() {
            return String.format("[Key-Id:%d]", id);
        }
    }

    final static class IdIndexData extends IndexData implements IByteArraySerializable {

        @Override public int size() {
            return 0x14;
        }

        @Override public int readFrom(byte[] buffer, int offset) {
            hash = ByteUtil.readLeInt(buffer, offset + 0x00);
            id = ByteUtil.readLeInt(buffer, offset + 0x04);
            sdsOffset = ByteUtil.readLeLong(buffer, offset + 0x08);
            sdsLength = ByteUtil.readLeInt(buffer, offset + 0x10);
            return 0x14;
        }

        @Override public void writeTo(byte[] buffer, int offset) {
            ByteUtil.writeLeInt(hash, buffer, offset + 0x00);
            ByteUtil.writeLeInt(id, buffer, offset + 0x04);
            ByteUtil.writeLeLong(sdsOffset, buffer, offset + 0x08);
            ByteUtil.writeLeInt(sdsLength, buffer, offset + 0x10);
        }
    }

    private static class HashFinder implements Comparable<discUtils.ntfs.SecurityDescriptors.HashIndexKey> {

        private final int toMatch;

        public HashFinder(int toMatch) {
            this.toMatch = toMatch;
        }

        @Override public int compareTo(discUtils.ntfs.SecurityDescriptors.HashIndexKey other) {
            return compareTo(other.hash);
        }

        public int compareTo(int otherHash) {
            return Integer.compare(toMatch, otherHash);
        }
    }
}
