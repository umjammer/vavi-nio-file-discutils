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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class ObjectIds {
    private final File _file;

    private final IndexView<DiscUtils.Ntfs.ObjectIds.IndexKey, ObjectIdRecord> _index;

    public ObjectIds(File file) {
        _file = file;
        _index = new IndexView<>(file.getIndex("$O"));
    }

    public Map<UUID, ObjectIdRecord> getAll() {
        Map<UUID, ObjectIdRecord> result = new HashMap<>();
        for (Map.Entry<DiscUtils.Ntfs.ObjectIds.IndexKey, ObjectIdRecord> record : _index.getEntries().entrySet()) {
            result.put(record.getKey().Id, record.getValue());
        }
        return result;
    }

    public void add(UUID objId, FileRecordReference mftRef, UUID birthId, UUID birthVolumeId, UUID birthDomainId) {
        DiscUtils.Ntfs.ObjectIds.IndexKey newKey = new DiscUtils.Ntfs.ObjectIds.IndexKey();
        newKey.Id = objId;
        ObjectIdRecord newData = new ObjectIdRecord();
        newData.MftReference = mftRef;
        newData.BirthObjectId = birthId;
        newData.BirthVolumeId = birthVolumeId;
        newData.BirthDomainId = birthDomainId;
        _index.set___idx(newKey, newData);
        _file.updateRecordInMft();
    }

    public void remove(UUID objId) {
        DiscUtils.Ntfs.ObjectIds.IndexKey key = new DiscUtils.Ntfs.ObjectIds.IndexKey();
        key.Id = objId;
        _index.remove(key);
        _file.updateRecordInMft();
    }

    public boolean tryGetValue(UUID objId, ObjectIdRecord[] value) {
        DiscUtils.Ntfs.ObjectIds.IndexKey key = new DiscUtils.Ntfs.ObjectIds.IndexKey();
        key.Id = objId;
        return _index.tryGetValue(key, value);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "OBJECT ID INDEX");
        for (Map.Entry<DiscUtils.Ntfs.ObjectIds.IndexKey, ObjectIdRecord> entry : _index.getEntries().entrySet()) {
            writer.println(indent + "  OBJECT ID INDEX ENTRY");
            writer.println(indent + "             Id: " + entry.getKey().Id);
            writer.println(indent + "  MFT Reference: " + entry.getValue().MftReference);
            writer.println(indent + "   Birth Volume: " + entry.getValue().BirthVolumeId);
            writer.println(indent + "       Birth Id: " + entry.getValue().BirthObjectId);
            writer.println(indent + "   Birth Domain: " + entry.getValue().BirthDomainId);
        }
    }

    public final static class IndexKey implements IByteArraySerializable {
        public UUID Id;

        public long getSize() {
            return 16;
        }

        public int readFrom(byte[] buffer, int offset) {
            Id = EndianUtilities.toGuidLittleEndian(buffer, offset + 0);
            return 16;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(Id, buffer, offset + 0);
        }

        public String toString() {
            return String.format("[Key-Id:%s]", Id);
        }
    }
}