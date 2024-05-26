//
// Copyright (c) 2016, Bianco Veigel
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

package discUtils.lvm;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.core.LogicalVolumeInfo;
import discUtils.core.PhysicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.partitions.BiosPartitionTypes;
import discUtils.core.partitions.GuidPartitionTypes;
import discUtils.core.partitions.PartitionInfo;

import static java.lang.System.getLogger;


/**
 * A class that understands Linux LVM structures, mapping physical volumes to
 * logical volumes.
 */
public class LogicalVolumeManager {

    private static final Logger logger = getLogger(LogicalVolumeManager.class.getName());

    private final List<PhysicalVolume> devices;

    private final List<MetadataVolumeGroupSection> volumeGroups;

    /**
     * Initializes a new instance of the LogicalVolumeManager class.
     *
     * @param disks The initial set of disks to manage.
     */
    public LogicalVolumeManager(List<VirtualDisk> disks) {
        devices = new ArrayList<>();
        volumeGroups = new ArrayList<>();
        for (VirtualDisk disk : disks) {
            if (disk.isPartitioned()) {
                for (PartitionInfo partition : disk.getPartitions().getPartitions()) {
                    PhysicalVolume[] pv = new PhysicalVolume[1];
                    if (PhysicalVolume.tryOpen(partition, pv)) {
                        devices.add(pv[0]);
                    }
                }
            } else {
                PhysicalVolume[] pv = new PhysicalVolume[1];
                if (PhysicalVolume.tryOpen(disk.getContent(), pv)) {
                    devices.add(pv[0]);
                }
            }
        }
        for (PhysicalVolume device : devices) {
            for (MetadataVolumeGroupSection vg : device.vgMetadata.parsedMetadata.volumeGroupSections) {
                if (Collections.binarySearch(volumeGroups, vg, Comparator.comparing(x -> x.id)) < 0) {
                    volumeGroups.add(vg);
logger.log(Level.DEBUG, "Lg: " + vg.id);
                }
            }
        }
    }

    /**
     * Determines if a physical volume contains LVM data.
     *
     * @param volumeInfo The volume to inspect.
     * @return {@code true} if the physical volume contains LVM data, else
     *         {@code false} .
     */
    public static boolean handlesPhysicalVolume(PhysicalVolumeInfo volumeInfo) {
        PartitionInfo partition = volumeInfo.getPartition();
        if (partition == null)
            return false;

        return partition.getBiosType() == BiosPartitionTypes.LinuxLvm || partition.getGuidType().equals(GuidPartitionTypes.LinuxLvm);
    }

    /**
     * Gets the logical volumes held across the set of managed disks.
     *
     * @return An array of logical volumes.
     */
    public List<LogicalVolumeInfo> getLogicalVolumes() {
        List<LogicalVolumeInfo> result = new ArrayList<>();
        for (MetadataVolumeGroupSection vg : volumeGroups) {
            for (MetadataLogicalVolumeSection lv : vg.logicalVolumes) {
                Map<String, PhysicalVolume> pvs = new HashMap<>();
                boolean allPvsAvailable = true;
                boolean segmentTypesSupported = true;
                for (MetadataSegmentSection segment : lv.segments) {
                    if (segment.type != SegmentType.Striped)
                        segmentTypesSupported = false;
                    for (MetadataStripe stripe : segment.stripes) {
                        String pvAlias = stripe.physicalVolumeName;
                        if (!pvs.containsKey(pvAlias)) {
                            MetadataPhysicalVolumeSection pvm = getPhysicalVolumeMetadata(vg, pvAlias);
                            if (pvm == null) {
                                allPvsAvailable = false;
                                break;
                            }
                            PhysicalVolume pv = getPhysicalVolume(pvm.id);
                            if (pv == null) {
                                allPvsAvailable = false;
                                break;
                            }
                            pvs.put(pvm.name, pv);
                        }
                    }
                    if (!allPvsAvailable || !segmentTypesSupported)
                        break;
                }
                if (allPvsAvailable && segmentTypesSupported) {
                    LogicalVolumeInfo lvi = new LogicalVolumeInfo(lv.identity,
                                                                  null,
                                                                  lv.open(pvs, vg.extentSize),
                                                                  lv.getExtentCount() * vg.extentSize *
                                                                                               PhysicalVolume.SECTOR_SIZE,
                                                                  (byte) 0,
                                                                  discUtils.core.LogicalVolumeStatus.Healthy);
                    result.add(lvi);
logger.log(Level.DEBUG, "Lv: " + lvi.getIdentity() + ", " + lv.getExtentCount() * vg.extentSize * PhysicalVolume.SECTOR_SIZE);
                }
            }
        }
        return result;
    }

    private PhysicalVolume getPhysicalVolume(String id) {
        for (PhysicalVolume pv : devices) {
            if (pv.pvHeader.uuid.equals(id))
                return pv;
        }
        return null;
    }

    private MetadataPhysicalVolumeSection getPhysicalVolumeMetadata(MetadataVolumeGroupSection vg, String name) {
        for (MetadataPhysicalVolumeSection pv : vg.physicalVolumes) {
            if (pv.name.equals(name))
                return pv;
        }
        return null;
    }
}
