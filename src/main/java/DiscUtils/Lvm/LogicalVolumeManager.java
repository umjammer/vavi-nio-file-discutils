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

package DiscUtils.Lvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import vavi.util.Debug;

import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.PhysicalVolumeInfo;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Partitions.BiosPartitionTypes;
import DiscUtils.Core.Partitions.GuidPartitionTypes;
import DiscUtils.Core.Partitions.PartitionInfo;


/**
 * A class that understands Linux LVM structures, mapping physical volumes to
 * logical volumes.
 */
public class LogicalVolumeManager {
    private List<PhysicalVolume> _devices;

    private List<MetadataVolumeGroupSection> _volumeGroups;

    /**
     * Initializes a new instance of the LogicalVolumeManager class.
     *
     * @param disks The initial set of disks to manage.
     */
    public LogicalVolumeManager(List<VirtualDisk> disks) {
        _devices = new ArrayList<>();
        _volumeGroups = new ArrayList<>();
        for (VirtualDisk disk : disks) {
            if (disk.isPartitioned()) {
                for (PartitionInfo partition : disk.getPartitions().getPartitions()) {
                    PhysicalVolume[] pv = new PhysicalVolume[1];
                    if (PhysicalVolume.tryOpen(partition, pv)) {
                        _devices.add(pv[0]);
                    }
                }
            } else {
                PhysicalVolume[] pv = new PhysicalVolume[1];
                if (PhysicalVolume.tryOpen(disk.getContent(), pv)) {
                    _devices.add(pv[0]);
                }
            }
        }
        for (PhysicalVolume device : _devices) {
            for (MetadataVolumeGroupSection vg : device.VgMetadata.ParsedMetadata.VolumeGroupSections) {
                if (Collections.binarySearch(_volumeGroups, vg, (x, y) -> x.Id.compareTo(y.Id)) < 0) {
                    _volumeGroups.add(vg);
Debug.println(Level.FINE, "Lg: " + vg.Id);
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
        for (MetadataVolumeGroupSection vg : _volumeGroups) {
            for (MetadataLogicalVolumeSection lv : vg.LogicalVolumes) {
                Map<String, PhysicalVolume> pvs = new HashMap<>();
                boolean allPvsAvailable = true;
                boolean segmentTypesSupported = true;
                for (MetadataSegmentSection segment : lv.Segments) {
                    if (segment.Type != SegmentType.Striped)
                        segmentTypesSupported = false;
                    for (MetadataStripe stripe : segment.Stripes) {
                        String pvAlias = stripe.PhysicalVolumeName;
                        if (!pvs.containsKey(pvAlias)) {
                            MetadataPhysicalVolumeSection pvm = getPhysicalVolumeMetadata(vg, pvAlias);
                            if (pvm == null) {
                                allPvsAvailable = false;
                                break;
                            }
                            PhysicalVolume pv = getPhysicalVolume(pvm.Id);
                            if (pv == null) {
                                allPvsAvailable = false;
                                break;
                            }
                            pvs.put(pvm.Name, pv);
                        }
                    }
                    if (!allPvsAvailable || !segmentTypesSupported)
                        break;
                }
                if (allPvsAvailable && segmentTypesSupported) {
                    LogicalVolumeInfo lvi = new LogicalVolumeInfo(lv.Identity,
                                                                  null,
                                                                  lv.open(pvs, vg.ExtentSize),
                                                                  lv.getExtentCount() * vg.ExtentSize *
                                                                                               PhysicalVolume.SECTOR_SIZE,
                                                                  (byte) 0,
                                                                  DiscUtils.Core.LogicalVolumeStatus.Healthy);
                    result.add(lvi);
Debug.println(Level.FINE, "Lv: " + lvi.getIdentity() + ", " + lv.getExtentCount() * vg.ExtentSize * PhysicalVolume.SECTOR_SIZE);
                }
            }
        }
        return result;
    }

    private PhysicalVolume getPhysicalVolume(String id) {
        for (PhysicalVolume pv : _devices) {
            if (pv.PvHeader.Uuid.equals(id))
                return pv;
        }
        return null;
    }

    private MetadataPhysicalVolumeSection getPhysicalVolumeMetadata(MetadataVolumeGroupSection vg, String name) {
        for (MetadataPhysicalVolumeSection pv : vg.PhysicalVolumes) {
            if (pv.Name.equals(name))
                return pv;
        }
        return null;
    }
}
