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

package discUtils.core.logicalDiskManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public class Database {

    private final Map<Long, DatabaseRecord> records;

    private final DatabaseHeader vmdb;

    public Database(Stream stream) {
        long dbStart = stream.position();
        byte[] buffer = new byte[Sizes.Sector];
        stream.read(buffer, 0, buffer.length);
        vmdb = new DatabaseHeader();
        vmdb.readFrom(buffer, 0);
        stream.position(dbStart + vmdb.headerSize);
        buffer = StreamUtilities.readExact(stream, vmdb.blockSize * vmdb.numVBlks);
        records = new HashMap<>();
        for (int i = 0; i < vmdb.numVBlks; ++i) {
            DatabaseRecord rec = DatabaseRecord.readFrom(buffer, new int[] { i * vmdb.blockSize});
            if (rec != null) {
                records.put(rec.id, rec);
            }
        }
    }

    public List<DiskRecord> getDisks() {
        return records.values()
                .stream()
                .filter(r -> r.recordType == RecordType.Disk)
                .map(r -> (DiskRecord) r)
                .collect(Collectors.toList());
    }

    public List<VolumeRecord> getVolumes() {
        return records.values()
                .stream()
                .filter(r -> r.recordType == RecordType.Volume)
                .map(r -> (VolumeRecord) r)
                .collect(Collectors.toList());
    }

    public DiskGroupRecord getDiskGroup(UUID guid) {
        for (DatabaseRecord record : records.values()) {
            if (record.recordType == RecordType.DiskGroup) {
                DiskGroupRecord dgRecord = (DiskGroupRecord) record;
                if (UUID.fromString(dgRecord.groupGuidString).equals(guid) || guid.equals(new UUID(0L, 0L))) {
                    return dgRecord;
                }
            }
        }
        return null;
    }

    public List<ComponentRecord> getVolumeComponents(long volumeId) {
        List<ComponentRecord> result = new ArrayList<>();
        for (DatabaseRecord record : records.values()) {
            if (record.recordType == RecordType.Component) {
                ComponentRecord cmpntRecord = (ComponentRecord) record;
                if (cmpntRecord.volumeId == volumeId) {
                    result.add(cmpntRecord);
                }
            }
        }
        return result;
    }

    public List<ExtentRecord> getComponentExtents(long componentId) {
        List<ExtentRecord> result = new ArrayList<>();
        for (DatabaseRecord record : records.values()) {
            if (record.recordType == RecordType.Extent) {
                ExtentRecord extentRecord = (ExtentRecord) record;
                if (extentRecord.componentId == componentId) {
                    result.add(extentRecord);
                }
            }
        }
        return result;
    }

    public DiskRecord getDisk(long diskId) {
        return (DiskRecord) records.get(diskId);
    }

    public VolumeRecord getVolume(long volumeId) {
        return (VolumeRecord) records.get(volumeId);
    }

    public VolumeRecord getVolume(UUID id) {
        return findRecord(r -> r.volumeGuid.equals(id), RecordType.Volume);
    }

    @SuppressWarnings({"unchecked"})
    public <T extends DatabaseRecord> T findRecord(Predicate<T> pred, RecordType typeId) {
        for (DatabaseRecord record : records.values()) {
            if (record.recordType == typeId) {
                T t = (T) record;
                if (pred.test(t)) {
                    return t;
                }
            }
        }
        return null;
    }
}
