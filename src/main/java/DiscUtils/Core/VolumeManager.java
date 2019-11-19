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

package DiscUtils.Core;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.UUID;

import DiscUtils.Core.Internal.LogicalVolumeFactory;
import DiscUtils.Core.Partitions.PartitionInfo;
import DiscUtils.Core.Partitions.PartitionTable;
import DiscUtils.Core.Raw.Disk;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.Stream;


/**
 * VolumeManager interprets partitions and other on-disk structures (possibly
 * combining multiple disks). Although file systems commonly are placed directly
 * within partitions on a disk, in some cases a logical volume manager / logical
 * disk manager may be used, to combine disk regions in multiple ways for data
 * redundancy or other purposes.
 */
public final class VolumeManager implements Serializable {
    private static final UUID EMPTY = new UUID(0L, 0L);

    private static ServiceLoader<LogicalVolumeFactory> s_logicalVolumeFactories;

    private final List<VirtualDisk> _disks;

    private boolean _needScan;

    private Map<String, PhysicalVolumeInfo> _physicalVolumes;

    private Map<String, LogicalVolumeInfo> _logicalVolumes;

    /**
     * Initializes a new instance of the VolumeManager class.
     */
    public VolumeManager() {
        _disks = new ArrayList<>();
        _physicalVolumes = new TreeMap<>();
        _logicalVolumes = new TreeMap<>();
    }

    /**
     * Initializes a new instance of the VolumeManager class.
     *
     * @param initialDisk The initial disk to add.
     */
    public VolumeManager(VirtualDisk initialDisk) {
        this();
        addDisk(initialDisk);
    }

    /**
     * Initializes a new instance of the VolumeManager class.
     *
     * @param initialDiskContent Content of the initial disk to add.
     */
    public VolumeManager(Stream initialDiskContent) {
        this();
        addDisk(initialDiskContent);
    }

    /**
     * Register new LogicalVolumeFactories detected in an assembly
     */
    static {
        s_logicalVolumeFactories = ServiceLoader.load(LogicalVolumeFactory.class);
    }

    /**
     * Gets the physical volumes held on a disk.
     *
     * @param diskContent The contents of the disk to inspect.
     * @return An array of volumes.By preference, use the form of this method that
     *         takes a disk parameter.If the disk isn't partitioned, this method
     *         returns the entire disk contents as a single volume.
     */
    public static Collection<PhysicalVolumeInfo> getPhysicalVolumes(Stream diskContent) {
        return getPhysicalVolumes(new Disk(diskContent, Ownership.None));
    }

    /**
     * Gets the physical volumes held on a disk.
     *
     * @param disk The disk to inspect.
     * @return An array of volumes.If the disk isn't partitioned, this method
     *         returns the entire disk contents as a single volume.
     */
    public static Collection<PhysicalVolumeInfo> getPhysicalVolumes(VirtualDisk disk) {
        return new VolumeManager(disk).getPhysicalVolumes();
    }

    /**
     * Adds a disk to the volume manager.
     *
     * @param disk The disk to add.
     * @return The GUID the volume manager will use to identify the disk.
     */
    public String addDisk(VirtualDisk disk) {
        _needScan = true;
        int ordinal = _disks.size();
        _disks.add(disk);
        return getDiskId(ordinal);
    }

    /**
     * Adds a disk to the volume manager.
     *
     * @param content The contents of the disk to add.
     * @return The GUID the volume manager will use to identify the disk.
     */
    public String addDisk(Stream content) {
        return addDisk(new Disk(content, Ownership.None));
    }

    /**
     * Gets the physical volumes from all disks added to this volume manager.
     *
     * @return An array of physical volumes.
     */
    public List<PhysicalVolumeInfo> getPhysicalVolumes() {
        if (_needScan) {
            scan();
        }

        return new ArrayList<>(_physicalVolumes.values());
    }

    /**
     * Gets the logical volumes from all disks added to this volume manager.
     *
     * @return An array of logical volumes.
     */
    public List<LogicalVolumeInfo> getLogicalVolumes() {
        if (_needScan) {
            scan();
        }

        return new ArrayList<>(_logicalVolumes.values());
    }

    /**
     * Gets a particular volume, based on it's identity.
     *
     * @param identity The volume's identity.
     * @return The volume information for the volume, or returns {@code null} .
     */
    public VolumeInfo getVolume(String identity) throws IOException {
        if (_needScan) {
            scan();
        }

        if (_physicalVolumes.containsKey(identity)) {
            return _physicalVolumes.get(identity);
        }

        if (_logicalVolumes.containsKey(identity)) {
            return _logicalVolumes.get(identity);
        }

        return null;
    }

    private static void mapPhysicalVolumes(List<PhysicalVolumeInfo> physicalVols, Map<String, LogicalVolumeInfo> result) {
        for (PhysicalVolumeInfo physicalVol : physicalVols) {
            LogicalVolumeInfo lvi = new LogicalVolumeInfo(physicalVol.getPartitionIdentity(),
                                                          physicalVol,
                                                          physicalVol::open,
                                                          physicalVol.getLength(),
                                                          physicalVol.getBiosType(),
                                                          LogicalVolumeStatus.Healthy);

            result.put(lvi.getIdentity(), lvi);
//Debug.println("Lp: " + lvi.getIdentity());
        }
    }

    /**
     * Scans all of the disks for their physical and logical volumes.
     */
    private void scan() {
        Map<String, PhysicalVolumeInfo> newPhysicalVolumes = scanForPhysicalVolumes();
        Map<String, LogicalVolumeInfo> newLogicalVolumes = scanForLogicalVolumes(newPhysicalVolumes.values());

        _physicalVolumes = newPhysicalVolumes;
        _logicalVolumes = newLogicalVolumes;

        _needScan = false;
    }

    private Map<String, LogicalVolumeInfo> scanForLogicalVolumes(Collection<PhysicalVolumeInfo> physicalVols) {
        List<PhysicalVolumeInfo> unhandledPhysical = new ArrayList<>();
        Map<String, LogicalVolumeInfo> result = new LinkedHashMap<>();

        for (PhysicalVolumeInfo pvi : physicalVols) {
            boolean handled = false;
            for (LogicalVolumeFactory volFactory : s_logicalVolumeFactories) {
                if (volFactory.handlesPhysicalVolume(pvi)) {
                    handled = true;
                    break;
                }
            }

            if (!handled) {
                unhandledPhysical.add(pvi);
            }
        }

        mapPhysicalVolumes(unhandledPhysical, result);

        for (LogicalVolumeFactory volFactory : s_logicalVolumeFactories) {
            volFactory.mapDisks(_disks, result);
        }

        return result;
    }

    private Map<String, PhysicalVolumeInfo> scanForPhysicalVolumes() {
        Map<String, PhysicalVolumeInfo> result = new LinkedHashMap<>();

        for (int i = 0; i < _disks.size(); ++i) {
            // First scan physical volumes
            VirtualDisk disk = _disks.get(i);
            String diskId = getDiskId(i);

            if (PartitionTable.isPartitioned(disk.getContent())) {
                for (PartitionTable table : PartitionTable.getPartitionTables(disk)) {
                    for (PartitionInfo part : table.getPartitions()) {
                        PhysicalVolumeInfo pvi = new PhysicalVolumeInfo(diskId, disk, part);
                        result.put(pvi.getIdentity(), pvi);
//Debug.println("P: " + pvi.getIdentity());
                    }
                }
            } else {
                PhysicalVolumeInfo pvi = new PhysicalVolumeInfo(diskId, disk);
                result.put(pvi.getIdentity(), pvi);
            }
        }

        return result;
    }

    private String getDiskId(int ordinal) {
        VirtualDisk disk = _disks.get(ordinal);
        if (disk.isPartitioned()) {
            UUID guid = disk.getPartitions().getDiskGuid();
            if (!guid.equals(EMPTY)) {
                return "DG" + String.format("{%s}", guid);
            }
        }

        int sig = disk.getSignature();
        if (sig != 0) {
            return "DS" + String.format("%8X", sig);
        }

        return "DO" + ordinal;
    }
}
