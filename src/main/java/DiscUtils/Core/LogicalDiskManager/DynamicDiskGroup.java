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

package DiscUtils.Core.LogicalDiskManager;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.LogicalVolumeStatus;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Partitions.BiosPartitionTypes;
import DiscUtils.Streams.ConcatStream;
import DiscUtils.Streams.MirrorStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StripedStream;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;


public class DynamicDiskGroup implements IDiagnosticTraceable {
    private final Database _database;

    private final Map<UUID, DynamicDisk> _disks;

    private final DiskGroupRecord _record;

    public DynamicDiskGroup(VirtualDisk disk) {
        _disks = new HashMap<>();
        DynamicDisk dynDisk = new DynamicDisk(disk);
        _database = dynDisk.getDatabase();
        _disks.put(dynDisk.getId(), dynDisk);
        _record = dynDisk.getDatabase().getDiskGroup(dynDisk.getGroupId());
    }

    public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "DISK GROUP (" + _record.Name + ")");
        writer.println(linePrefix + "  Name: " + _record.Name);
        writer.println(linePrefix + "  Flags: 0x" + String.format("%4x", _record.Flags & 0xFFF0));
        writer.println(linePrefix + "  Database Id: " + _record.Id);
        writer.println(linePrefix + "  Guid: " + _record.GroupGuidString);
        writer.println();
        writer.println(linePrefix + "  DISKS");
        for (DiskRecord disk : _database.getDisks()) {
            writer.println(linePrefix + "    DISK (" + disk.Name + ")");
            writer.println(linePrefix + "      Name: " + disk.Name);
            writer.println(linePrefix + "      Flags: 0x" + String.format("%4x", disk.Flags & 0xFFF0));
            writer.println(linePrefix + "      Database Id: " + disk.Id);
            writer.println(linePrefix + "      Guid: " + disk.DiskGuidString);
            if (_disks.containsKey(UUID.fromString(disk.DiskGuidString))) {
                DynamicDisk dynDisk = _disks.get(UUID.fromString(disk.DiskGuidString));
                writer.println(linePrefix + "      PRIVATE HEADER");
                dynDisk.dump(writer, linePrefix + "        ");
            }

        }
        writer.println(linePrefix + "  VOLUMES");
        for (VolumeRecord vol : _database.getVolumes()) {
            writer.println(linePrefix + "    VOLUME (" + vol.Name + ")");
            writer.println(linePrefix + "      Name: " + vol.Name);
            writer.println(linePrefix + "      BIOS Type: " + String.format("%2x", vol.BiosType) + " [" +
                           BiosPartitionTypes.toString(vol.BiosType) + "]");
            writer.println(linePrefix + "      Flags: 0x" + String.format("%4x", vol.Flags & 0xFFF0));
            writer.println(linePrefix + "      Database Id: " + vol.Id);
            writer.println(linePrefix + "      Guid: " + vol.VolumeGuid);
            writer.println(linePrefix + "      State: " + vol.ActiveString);
            writer.println(linePrefix + "      Drive Hint: " + vol.MountHint);
            writer.println(linePrefix + "      Num Components: " + vol.ComponentCount);
            writer.println(linePrefix + "      Link Id: " + vol.PartitionComponentLink);
            writer.println(linePrefix + "      COMPONENTS");
            for (ComponentRecord cmpnt : _database.getVolumeComponents(vol.Id)) {
                writer.println(linePrefix + "        COMPONENT (" + cmpnt.Name + ")");
                writer.println(linePrefix + "          Name: " + cmpnt.Name);
                writer.println(linePrefix + "          Flags: 0x" + String.format("%4x", cmpnt.Flags & 0xFFF0));
                writer.println(linePrefix + "          Database Id: " + cmpnt.Id);
                writer.println(linePrefix + "          State: " + cmpnt.StatusString);
                writer.println(linePrefix + "          Mode: " + cmpnt.MergeType);
                writer.println(linePrefix + "          Num Extents: " + cmpnt.NumExtents);
                writer.println(linePrefix + "          Link Id: " + cmpnt.LinkId);
                writer.println(linePrefix + "          Stripe Size: " + cmpnt.StripeSizeSectors + " (Sectors)");
                writer.println(linePrefix + "          Stripe Stride: " + cmpnt.StripeStride);
                writer.println(linePrefix + "          EXTENTS");
                for (ExtentRecord extent : _database.getComponentExtents(cmpnt.Id)) {
                    writer.println(linePrefix + "            EXTENT (" + extent.Name + ")");
                    writer.println(linePrefix + "              Name: " + extent.Name);
                    writer.println(linePrefix + "              Flags: 0x" + String.format("%4x", extent.Flags & 0xFFF0));
                    writer.println(linePrefix + "              Database Id: " + extent.Id);
                    writer.println(linePrefix + "              Disk Offset: " + extent.DiskOffsetLba + " (Sectors)");
                    writer.println(linePrefix + "              Volume Offset: " + extent.OffsetInVolumeLba + " (Sectors)");
                    writer.println(linePrefix + "              Size: " + extent.SizeLba + " (Sectors)");
                    writer.println(linePrefix + "              Component Id: " + extent.ComponentId);
                    writer.println(linePrefix + "              Disk Id: " + extent.DiskId);
                    writer.println(linePrefix + "              Link Id: " + extent.PartitionComponentLink);
                    writer.println(linePrefix + "              Interleave Order: " + extent.InterleaveOrder);
                }
            }
        }
    }

    public void add(VirtualDisk disk) {
        DynamicDisk dynDisk = new DynamicDisk(disk);
        _disks.put(dynDisk.getId(), dynDisk);
    }

    public DynamicVolume[] getVolumes() {
        List<DynamicVolume> vols = new ArrayList<>();
        for (VolumeRecord record : _database.getVolumes()) {
            vols.add(new DynamicVolume(this, record.VolumeGuid));
        }
        return vols.toArray(new DynamicVolume[0]);
    }

    public VolumeRecord getVolume(UUID volume) {
        return _database.getVolume(volume);
    }

    public LogicalVolumeStatus getVolumeStatus(long volumeId) {
        return getVolumeStatus(_database.getVolume(volumeId));
    }

    public SparseStream openVolume(long volumeId) {
        return openVolume(_database.getVolume(volumeId));
    }

    private static Comparator<ExtentRecord> ExtentOffsets = (x, y) -> {
        if (x.OffsetInVolumeLba > y.OffsetInVolumeLba) {
            return 1;
        }

        if (x.OffsetInVolumeLba < y.OffsetInVolumeLba) {
            return -1;
        }

        return 0;
    };

    private static Comparator<ExtentRecord> ExtentInterleaveOrder = (x, y) -> {
        if (x.InterleaveOrder > y.InterleaveOrder) {
            return 1;
        }

        if (x.InterleaveOrder < y.InterleaveOrder) {
            return -1;
        }

        return 0;
    };

    private static LogicalVolumeStatus worstOf(LogicalVolumeStatus x, LogicalVolumeStatus y) {
        return LogicalVolumeStatus.values()[Math.max(x.ordinal(), y.ordinal())];
    }

    private LogicalVolumeStatus getVolumeStatus(VolumeRecord volume) {
        int numFailed = 0;
        long numOK = 0;
        LogicalVolumeStatus worst = LogicalVolumeStatus.Healthy;
        for (ComponentRecord cmpnt : _database.getVolumeComponents(volume.Id)) {
            LogicalVolumeStatus cmpntStatus = getComponentStatus(cmpnt);
            worst = worstOf(worst, cmpntStatus);
            if (cmpntStatus == LogicalVolumeStatus.Failed) {
                numFailed++;
            } else {
                numOK++;
            }
        }
        if (numOK < 1) {
            return LogicalVolumeStatus.Failed;
        }

        if (numOK == volume.ComponentCount) {
            return worst;
        }

        return LogicalVolumeStatus.FailedRedundancy;
    }

    private LogicalVolumeStatus getComponentStatus(ComponentRecord cmpnt) {
        // NOTE: no support for RAID, so either valid or failed...
        LogicalVolumeStatus status = LogicalVolumeStatus.Healthy;
        for (ExtentRecord extent : _database.getComponentExtents(cmpnt.Id)) {
            DiskRecord disk = _database.getDisk(extent.DiskId);
            if (!_disks.containsKey(UUID.fromString(disk.DiskGuidString))) {
                status = LogicalVolumeStatus.Failed;
                break;
            }

        }
        return status;
    }

    private SparseStream openExtent(ExtentRecord extent) {
        DiskRecord disk = _database.getDisk(extent.DiskId);
        DynamicDisk diskObj = _disks.get(UUID.fromString(disk.DiskGuidString));
        return new SubStream(diskObj.getContent(),
                             Ownership.None,
                             (diskObj.getDataOffset() + extent.DiskOffsetLba) * Sizes.Sector,
                             extent.SizeLba * Sizes.Sector);
    }

    private SparseStream openComponent(ComponentRecord component) {
        if (component.MergeType == ExtentMergeType.Concatenated) {
            List<ExtentRecord> extents = new ArrayList<>(_database.getComponentExtents(component.Id));
            extents.sort(ExtentOffsets);
            // Sanity Check...
            long pos = 0;
            for (ExtentRecord extent : extents) {
                if (extent.OffsetInVolumeLba != pos) {
                    throw new dotnet4j.io.IOException("Volume extents are non-contiguous");
                }

                pos += extent.SizeLba;
            }
            List<SparseStream> streams = new ArrayList<>();
            for (ExtentRecord extent : extents) {
                streams.add(openExtent(extent));
            }
            return new ConcatStream(Ownership.Dispose, streams);
        }

        if (component.MergeType == ExtentMergeType.Interleaved) {
            List<ExtentRecord> extents = new ArrayList<>(_database.getComponentExtents(component.Id));
            extents.sort(ExtentInterleaveOrder);
            List<SparseStream> streams = new ArrayList<>();
            for (ExtentRecord extent : extents) {
                streams.add(openExtent(extent));
            }
            return new StripedStream(component.StripeSizeSectors * Sizes.Sector, Ownership.Dispose, streams);
        }

        throw new UnsupportedOperationException("Unknown component mode: " + component.MergeType);
    }

    private SparseStream openVolume(VolumeRecord volume) {
        List<SparseStream> cmpntStreams = new ArrayList<>();
        for (ComponentRecord component : _database.getVolumeComponents(volume.Id)) {
            if (getComponentStatus(component) == LogicalVolumeStatus.Healthy) {
                cmpntStreams.add(openComponent(component));
            }

        }
        if (cmpntStreams.size() < 1) {
            throw new dotnet4j.io.IOException("Volume with no associated or healthy components");
        }

        if (cmpntStreams.size() == 1) {
            return cmpntStreams.get(0);
        }

        return new MirrorStream(Ownership.Dispose, cmpntStreams);
    }
}
