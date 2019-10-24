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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.PhysicalVolumeInfo;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Partitions.BiosPartitionTypes;
import DiscUtils.Core.Partitions.GuidPartitionTypes;
import DiscUtils.Core.Partitions.PartitionInfo;


/**
 * A class that understands Windows LDM structures, mapping physical volumes to
 * logical volumes.
 */
public class DynamicDiskManager implements IDiagnosticTraceable {
    private final Map<String, DynamicDiskGroup> _groups;

    /**
     * Initializes a new instance of the DynamicDiskManager class.
     *
     * @param disks The initial set of disks to manage.
     */
    public DynamicDiskManager(VirtualDisk... disks) {
        _groups = new HashMap<>();
        for (VirtualDisk disk : disks) {
            add(disk);
        }
    }

    /**
     * Writes a diagnostic report about the state of the disk manager.
     *
     * @param writer The writer to send the report to.
     * @param linePrefix The prefix to place at the start of each line.
     */
    public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "DISK GROUPS");
        for (DynamicDiskGroup group : _groups.values()) {
            group.dump(writer, linePrefix + "  ");
        }
    }

    /**
     * Determines if a physical volume contains LDM data.
     *
     * @param volumeInfo The volume to inspect.
     * @return
     *         {@code true}
     *         if the physical volume contains LDM data, else
     *         {@code false}
     *         .
     */
    public static boolean handlesPhysicalVolume(PhysicalVolumeInfo volumeInfo) {
        PartitionInfo pi = volumeInfo.getPartition();
        if (pi != null) {
            return isLdmPartition(pi);
        }

        return false;
    }

    /**
     * Determines if a disk is 'dynamic' (i.e. contains LDM volumes).
     *
     * @param disk The disk to inspect.
     * @return
     *         {@code true}
     *         if the disk contains LDM volumes, else
     *         {@code false}
     *         .
     */
    public static boolean isDynamicDisk(VirtualDisk disk) {
        if (disk.isPartitioned()) {
            for (PartitionInfo partition : disk.getPartitions().getPartitions()) {
                if (isLdmPartition(partition)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Adds a new disk to be managed.
     *
     * @param disk The disk to manage.
     */
    public void add(VirtualDisk disk) {
        PrivateHeader header = DynamicDisk.getPrivateHeader(disk);
        if (_groups.containsKey(header.DiskGroupId)) {
            DynamicDiskGroup group = _groups.get(header.DiskGroupId);
            group.add(disk);
        } else {
            DynamicDiskGroup group = new DynamicDiskGroup(disk);
            _groups.put(header.DiskGroupId, group);
        }
    }

    /**
     * Gets the logical volumes held across the set of managed disks.
     *
     * @return An array of logical volumes.
     */
    public List<LogicalVolumeInfo> getLogicalVolumes() {
        List<LogicalVolumeInfo> result = new ArrayList<>();
        for (DynamicDiskGroup group : _groups.values()) {
            for (DynamicVolume volume : group.getVolumes()) {
                LogicalVolumeInfo lvi = new LogicalVolumeInfo(volume
                        .getIdentity(), null, () -> {
                            try {
                                return volume.open();
                            } catch (IOException e) {
                                throw new dotnet4j.io.IOException(e);
                            }
                        }, volume.getLength(), volume.getBiosType(), volume.getStatus());
                result.add(lvi);
            }
        }
        return result;
    }

    private static boolean isLdmPartition(PartitionInfo partition) {
        return partition.getBiosType() == BiosPartitionTypes.WindowsDynamicVolume ||
               partition.getGuidType() == GuidPartitionTypes.WindowsLdmMetadata ||
               partition.getGuidType() == GuidPartitionTypes.WindowsLdmData;
    }
}
