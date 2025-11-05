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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import discUtils.core.IDiagnosticTraceable;
import discUtils.core.LogicalVolumeStatus;
import discUtils.core.VirtualDisk;
import discUtils.core.partitions.BiosPartitionTypes;
import discUtils.streams.ConcatStream;
import discUtils.streams.MirrorStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StripedStream;
import discUtils.streams.SubStream;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;


public class DynamicDiskGroup implements IDiagnosticTraceable {

    private final Database database;

    private final Map<UUID, DynamicDisk> disks;

    private final DiskGroupRecord record;

    public DynamicDiskGroup(VirtualDisk disk) {
        disks = new HashMap<>();
        DynamicDisk dynDisk = new DynamicDisk(disk);
        database = dynDisk.getDatabase();
        disks.put(dynDisk.getId(), dynDisk);
        record = dynDisk.getDatabase().getDiskGroup(dynDisk.getGroupId());
    }

    @Override
    public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "DISK GROUP (" + record.name + ")");
        writer.println(linePrefix + "  Name: " + record.name);
        writer.println(linePrefix + "  flags: 0x" + "%4x".formatted(record.flags & 0xFFF0));
        writer.println(linePrefix + "  Database Id: " + record.id);
        writer.println(linePrefix + "  Guid: " + record.groupGuidString);
        writer.println();
        writer.println(linePrefix + "  DISKS");
        for (DiskRecord disk : database.getDisks()) {
            writer.println(linePrefix + "    DISK (" + disk.name + ")");
            writer.println(linePrefix + "      Name: " + disk.name);
            writer.println(linePrefix + "      flags: 0x" + "%4x".formatted(disk.flags & 0xFFF0));
            writer.println(linePrefix + "      Database Id: " + disk.id);
            writer.println(linePrefix + "      Guid: " + disk.diskGuidString);
            if (disks.containsKey(UUID.fromString(disk.diskGuidString))) {
                DynamicDisk dynDisk = disks.get(UUID.fromString(disk.diskGuidString));
                writer.println(linePrefix + "      PRIVATE HEADER");
                dynDisk.dump(writer, linePrefix + "        ");
            }

        }
        writer.println(linePrefix + "  VOLUMES");
        for (VolumeRecord vol : database.getVolumes()) {
            writer.println(linePrefix + "    VOLUME (" + vol.name + ")");
            writer.println(linePrefix + "      Name: " + vol.name);
            writer.println(linePrefix + "      BIOS Type: " + "%2x".formatted(vol.biosType) + " [" +
                           BiosPartitionTypes.toString(vol.biosType) + "]");
            writer.println(linePrefix + "      flags: 0x" + "%4x".formatted(vol.flags & 0xFFF0));
            writer.println(linePrefix + "      Database Id: " + vol.id);
            writer.println(linePrefix + "      Guid: " + vol.volumeGuid);
            writer.println(linePrefix + "      State: " + vol.activeString);
            writer.println(linePrefix + "      Drive Hint: " + vol.mountHint);
            writer.println(linePrefix + "      Num Components: " + vol.componentCount);
            writer.println(linePrefix + "      Link Id: " + vol.partitionComponentLink);
            writer.println(linePrefix + "      COMPONENTS");
            for (ComponentRecord cmpnt : database.getVolumeComponents(vol.id)) {
                writer.println(linePrefix + "        COMPONENT (" + cmpnt.name + ")");
                writer.println(linePrefix + "          Name: " + cmpnt.name);
                writer.println(linePrefix + "          flags: 0x" + "%4x".formatted(cmpnt.flags & 0xFFF0));
                writer.println(linePrefix + "          Database Id: " + cmpnt.id);
                writer.println(linePrefix + "          State: " + cmpnt.statusString);
                writer.println(linePrefix + "          Mode: " + cmpnt.mergeType);
                writer.println(linePrefix + "          Num Extents: " + cmpnt.numExtents);
                writer.println(linePrefix + "          Link Id: " + cmpnt.linkId);
                writer.println(linePrefix + "          Stripe Size: " + cmpnt.stripeSizeSectors + " (Sectors)");
                writer.println(linePrefix + "          Stripe Stride: " + cmpnt.stripeStride);
                writer.println(linePrefix + "          EXTENTS");
                for (ExtentRecord extent : database.getComponentExtents(cmpnt.id)) {
                    writer.println(linePrefix + "            EXTENT (" + extent.name + ")");
                    writer.println(linePrefix + "              Name: " + extent.name);
                    writer.println(linePrefix + "              flags: 0x" + "%4x".formatted(extent.flags & 0xFFF0));
                    writer.println(linePrefix + "              Database Id: " + extent.id);
                    writer.println(linePrefix + "              Disk Offset: " + extent.diskOffsetLba + " (Sectors)");
                    writer.println(linePrefix + "              Volume Offset: " + extent.offsetInVolumeLba + " (Sectors)");
                    writer.println(linePrefix + "              Size: " + extent.sizeLba + " (Sectors)");
                    writer.println(linePrefix + "              Component Id: " + extent.componentId);
                    writer.println(linePrefix + "              Disk Id: " + extent.diskId);
                    writer.println(linePrefix + "              Link Id: " + extent.partitionComponentLink);
                    writer.println(linePrefix + "              Interleave Order: " + extent.interleaveOrder);
                }
            }
        }
    }

    public void add(VirtualDisk disk) {
        DynamicDisk dynDisk = new DynamicDisk(disk);
        disks.put(dynDisk.getId(), dynDisk);
    }

    public DynamicVolume[] getVolumes() {
        List<DynamicVolume> vols = new ArrayList<>();
        for (VolumeRecord record : database.getVolumes()) {
            vols.add(new DynamicVolume(this, record.volumeGuid));
        }
        return vols.toArray(new DynamicVolume[0]);
    }

    public VolumeRecord getVolume(UUID volume) {
        return database.getVolume(volume);
    }

    public LogicalVolumeStatus getVolumeStatus(long volumeId) {
        return getVolumeStatus(database.getVolume(volumeId));
    }

    public SparseStream openVolume(long volumeId) {
        return openVolume(database.getVolume(volumeId));
    }

    private static Comparator<ExtentRecord> ExtentOffsets = Comparator.comparingLong(x -> x.offsetInVolumeLba);

    private static Comparator<ExtentRecord> ExtentInterleaveOrder = Comparator.comparingLong(x -> x.interleaveOrder);

    private static LogicalVolumeStatus worstOf(LogicalVolumeStatus x, LogicalVolumeStatus y) {
        return LogicalVolumeStatus.values()[Math.max(x.ordinal(), y.ordinal())];
    }

    private LogicalVolumeStatus getVolumeStatus(VolumeRecord volume) {
        int numFailed = 0;
        long numOK = 0;
        LogicalVolumeStatus worst = LogicalVolumeStatus.Healthy;
        for (ComponentRecord cmpnt : database.getVolumeComponents(volume.id)) {
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

        if (numOK == volume.componentCount) {
            return worst;
        }

        return LogicalVolumeStatus.FailedRedundancy;
    }

    private LogicalVolumeStatus getComponentStatus(ComponentRecord cmpnt) {
        // NOTE: no support for RAID, so either valid or failed...
        LogicalVolumeStatus status = LogicalVolumeStatus.Healthy;
        for (ExtentRecord extent : database.getComponentExtents(cmpnt.id)) {
            DiskRecord disk = database.getDisk(extent.diskId);
            if (!disks.containsKey(UUID.fromString(disk.diskGuidString))) {
                status = LogicalVolumeStatus.Failed;
                break;
            }

        }
        return status;
    }

    private SparseStream openExtent(ExtentRecord extent) {
        DiskRecord disk = database.getDisk(extent.diskId);
        DynamicDisk diskObj = disks.get(UUID.fromString(disk.diskGuidString));
        return new SubStream(diskObj.getContent(),
                             Ownership.None,
                             (diskObj.getDataOffset() + extent.diskOffsetLba) * Sizes.Sector,
                             extent.sizeLba * Sizes.Sector);
    }

    private SparseStream openComponent(ComponentRecord component) {
        if (component.mergeType == ExtentMergeType.Concatenated) {
            List<ExtentRecord> extents = new ArrayList<>(database.getComponentExtents(component.id));
            extents.sort(ExtentOffsets);
            // Sanity Check...
            long pos = 0;
            for (ExtentRecord extent : extents) {
                if (extent.offsetInVolumeLba != pos) {
                    throw new dotnet4j.io.IOException("Volume extents are non-contiguous");
                }

                pos += extent.sizeLba;
            }
            List<SparseStream> streams = new ArrayList<>();
            for (ExtentRecord extent : extents) {
                streams.add(openExtent(extent));
            }
            return new ConcatStream(Ownership.Dispose, streams);
        }

        if (component.mergeType == ExtentMergeType.Interleaved) {
            List<ExtentRecord> extents = new ArrayList<>(database.getComponentExtents(component.id));
            extents.sort(ExtentInterleaveOrder);
            List<SparseStream> streams = new ArrayList<>();
            for (ExtentRecord extent : extents) {
                streams.add(openExtent(extent));
            }
            return new StripedStream(component.stripeSizeSectors * Sizes.Sector, Ownership.Dispose, streams);
        }

        throw new UnsupportedOperationException("Unknown component mode: " + component.mergeType);
    }

    private SparseStream openVolume(VolumeRecord volume) {
        List<SparseStream> cmpntStreams = new ArrayList<>();
        for (ComponentRecord component : database.getVolumeComponents(volume.id)) {
            if (getComponentStatus(component) == LogicalVolumeStatus.Healthy) {
                cmpntStreams.add(openComponent(component));
            }

        }
        if (cmpntStreams.isEmpty()) {
            throw new dotnet4j.io.IOException("Volume with no associated or healthy components");
        }

        if (cmpntStreams.size() == 1) {
            return cmpntStreams.get(0);
        }

        return new MirrorStream(Ownership.Dispose, cmpntStreams);
    }
}
