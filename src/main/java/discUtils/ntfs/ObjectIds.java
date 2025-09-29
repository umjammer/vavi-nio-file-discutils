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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import dotnet4j.util.compat.Tuple;
import vavi.util.ByteUtil;


public final class ObjectIds {

    private final File file;

    private final IndexView<discUtils.ntfs.ObjectIds.IndexKey, ObjectIdRecord> index;

    public ObjectIds(File file) {
        this.file = file;
        index = new IndexView<>(discUtils.ntfs.ObjectIds.IndexKey.class, ObjectIdRecord.class, file.getIndex("$O"));
    }

    public Map<UUID, ObjectIdRecord> getAll() {
        Map<UUID, ObjectIdRecord> result = new HashMap<>();
        for (Tuple<discUtils.ntfs.ObjectIds.IndexKey, ObjectIdRecord> record : index.getEntries()) {
            result.put(record.getKey().id, record.getValue());
        }
        return result;
    }

    public void add(UUID objId, FileRecordReference mftRef, UUID birthId, UUID birthVolumeId, UUID birthDomainId) {
        discUtils.ntfs.ObjectIds.IndexKey newKey = new discUtils.ntfs.ObjectIds.IndexKey();
        newKey.id = objId;
        ObjectIdRecord newData = new ObjectIdRecord();
        newData.mftReference = mftRef;
        newData.birthObjectId = birthId;
        newData.birthVolumeId = birthVolumeId;
        newData.birthDomainId = birthDomainId;
        index.put(newKey, newData);
        file.updateRecordInMft();
    }

    public void remove(UUID objId) {
        discUtils.ntfs.ObjectIds.IndexKey key = new discUtils.ntfs.ObjectIds.IndexKey();
        key.id = objId;
        index.remove(key);
        file.updateRecordInMft();
    }

    public boolean tryGetValue(UUID objId, ObjectIdRecord[] value) {
        discUtils.ntfs.ObjectIds.IndexKey key = new discUtils.ntfs.ObjectIds.IndexKey();
        key.id = objId;
        return index.tryGetValue(key, value);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "OBJECT ID INDEX");
        for (Tuple<discUtils.ntfs.ObjectIds.IndexKey, ObjectIdRecord> entry : index.getEntries()) {
            writer.println(indent + "  OBJECT ID INDEX ENTRY");
            writer.println(indent + "             Id: " + entry.getKey().id);
            writer.println(indent + "  MFT Reference: " + entry.getValue().mftReference);
            writer.println(indent + "   Birth Volume: " + entry.getValue().birthVolumeId);
            writer.println(indent + "       Birth Id: " + entry.getValue().birthObjectId);
            writer.println(indent + "   Birth Domain: " + entry.getValue().birthDomainId);
        }
    }

    public final static class IndexKey implements IByteArraySerializable {

        public UUID id;

        @Override public int size() {
            return 16;
        }

        @Override public int readFrom(byte[] buffer, int offset) {
            id = ByteUtil.readLeUUID(buffer, offset + 0);
            return 16;
        }

        @Override public void writeTo(byte[] buffer, int offset) {
            ByteUtil.writeLeUUID(id, buffer, offset + 0);
        }

        @Override public String toString() {
            return "[Key-Id:%s]".formatted(id);
        }
    }
}
