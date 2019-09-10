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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import DiscUtils.Core.CoreCompat.ReflectionHelper;
import DiscUtils.Core.Internal.LogicalVolumeFactory;
import DiscUtils.Core.Internal.LogicalVolumeFactoryAttribute;
import DiscUtils.Core.Partitions.PartitionInfo;
import DiscUtils.Core.Partitions.PartitionTable;
import DiscUtils.Core.Raw.Disk;
import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * VolumeManager interprets partitions and other on-disk structures (possibly
 * combining multiple disks).
 * Although file systems commonly are placed directly within partitions on a
 * disk, in some
 * cases a logical volume manager / logical disk manager may be used, to combine
 * disk regions in multiple
 * ways for data redundancy or other purposes.
 */
public final class VolumeManager implements Serializable {
    private static List<LogicalVolumeFactory> s_logicalVolumeFactories;

    private final List<VirtualDisk> _disks;

    private boolean _needScan;

    private Map<String, PhysicalVolumeInfo> _physicalVolumes;

    private Map<String, LogicalVolumeInfo> _logicalVolumes;

    private static final List<Class<VolumeManager>> _coreAssembly = Arrays.asList(VolumeManager.class);

    /**
     * Initializes a new instance of the VolumeManager class.
     */
    public VolumeManager() {
        _disks = new ArrayList<>();
        _physicalVolumes = new HashMap<>();
        _logicalVolumes = new HashMap<>();
    }

    /**
     * Initializes a new instance of the VolumeManager class.
     * 
     * @param initialDisk The initial disk to add.
     */
    public VolumeManager(VirtualDisk initialDisk) throws IOException {
        this();
        addDisk(initialDisk);
    }

    /**
     * Initializes a new instance of the VolumeManager class.
     * 
     * @param initialDiskContent Content of the initial disk to add.
     */
    public VolumeManager(Stream initialDiskContent) throws IOException {
        this();
        addDisk(initialDiskContent);
    }

    private static List<LogicalVolumeFactory> getLogicalVolumeFactories() throws IOException {
        if (s_logicalVolumeFactories == null) {
            List<LogicalVolumeFactory> factories = new ArrayList<>();
            factories.addAll(getLogicalVolumeFactories(_coreAssembly));
            s_logicalVolumeFactories = factories;
        }

        return s_logicalVolumeFactories;
    }

    private static List<LogicalVolumeFactory> getLogicalVolumeFactories(List<Class<VolumeManager>> assembly) {
        try {
            List<LogicalVolumeFactory> result = new ArrayList<>();
            for (Class<VolumeManager> type : assembly) {
                for (LogicalVolumeFactoryAttribute attr : ReflectionHelper
                        .getCustomAttributes(type, LogicalVolumeFactoryAttribute.class, false)) {
                    result.add(LogicalVolumeFactory.class.cast(type.newInstance()));
                }
            }
            return result;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Register new LogicalVolumeFactories detected in an assembly
     * 
     * @param assembly The assembly to inspect
     */
    public static void registerLogicalVolumeFactory(List<Class<VolumeManager>> assembly) throws IOException {
        if (assembly == _coreAssembly)
            return;

        getLogicalVolumeFactories().addAll(getLogicalVolumeFactories(assembly));
    }

    /**
     * Gets the physical volumes held on a disk.
     * 
     * @param diskContent The contents of the disk to inspect.
     * @return An array of volumes.By preference, use the form of this method
     *         that takes a disk parameter.If the disk isn't partitioned, this
     *         method returns the entire disk contents
     *         as a single volume.
     */
    public static Collection<PhysicalVolumeInfo> getPhysicalVolumes(Stream diskContent) throws IOException {
        return getPhysicalVolumes(new Disk(diskContent, Ownership.None));
    }

    /**
     * Gets the physical volumes held on a disk.
     * 
     * @param disk The disk to inspect.
     * @return An array of volumes.If the disk isn't partitioned, this method
     *         returns the entire disk contents
     *         as a single volume.
     */
    public static Collection<PhysicalVolumeInfo> getPhysicalVolumes(VirtualDisk disk) throws IOException {
        return new VolumeManager(disk).getPhysicalVolumes();
    }

    /**
     * Adds a disk to the volume manager.
     * 
     * @param disk The disk to add.
     * @return The GUID the volume manager will use to identify the disk.
     */
    public String addDisk(VirtualDisk disk) throws IOException {
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
    public String addDisk(Stream content) throws IOException {
        return addDisk(new Disk(content, Ownership.None));
    }

    /**
     * Gets the physical volumes from all disks added to this volume manager.
     * 
     * @return An array of physical volumes.
     */
    public Collection<PhysicalVolumeInfo> getPhysicalVolumes() throws IOException {
        if (_needScan) {
            scan();
        }

        return _physicalVolumes.values();
    }

    /**
     * Gets the logical volumes from all disks added to this volume manager.
     * 
     * @return An array of logical volumes.
     */
    public Collection<LogicalVolumeInfo> getLogicalVolumes() throws IOException {
        if (_needScan) {
            scan();
        }

        return _logicalVolumes.values();
    }

    /**
     * Gets a particular volume, based on it's identity.
     * 
     * @param identity The volume's identity.
     * @return The volume information for the volume, or returns
     *         {@code null}
     *         .
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

    private static void mapPhysicalVolumes(List<PhysicalVolumeInfo> physicalVols,
                                           Map<String, LogicalVolumeInfo> result) {
        for (PhysicalVolumeInfo physicalVol : physicalVols) {
            LogicalVolumeInfo lvi = new LogicalVolumeInfo(physicalVol.getPartitionIdentity(),
                                                          physicalVol,
                                                          physicalVol::open,
                                                          physicalVol.getLength(),
                                                          physicalVol.getBiosType(),
                                                          LogicalVolumeStatus.Healthy);
            result.put(lvi.getIdentity(), lvi);
        }
    }

    /**
     * Scans all of the disks for their physical and logical volumes.
     */
    private void scan() throws IOException {
        Map<String, PhysicalVolumeInfo> newPhysicalVolumes = scanForPhysicalVolumes();
        Map<String, LogicalVolumeInfo> newLogicalVolumes = scanForLogicalVolumes(newPhysicalVolumes.values());
        _physicalVolumes = newPhysicalVolumes;
        _logicalVolumes = newLogicalVolumes;
        _needScan = false;
    }

    private Map<String, LogicalVolumeInfo> scanForLogicalVolumes(Collection<PhysicalVolumeInfo> physicalVols) throws IOException {
        List<PhysicalVolumeInfo> unhandledPhysical = new ArrayList<>();
        Map<String, LogicalVolumeInfo> result = new HashMap<>();
        for (PhysicalVolumeInfo pvi : physicalVols) {
            boolean handled = false;
            for (LogicalVolumeFactory volFactory : getLogicalVolumeFactories()) {
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
        for (Object __dummyForeachVar5 : getLogicalVolumeFactories()) {
            LogicalVolumeFactory volFactory = (LogicalVolumeFactory) __dummyForeachVar5;
            volFactory.mapDisks(_disks, result);
        }
        return result;
    }

    private Map<String, PhysicalVolumeInfo> scanForPhysicalVolumes() throws IOException {
        Map<String, PhysicalVolumeInfo> result = new HashMap<>();
        for (int i = 0; i < _disks.size(); ++i) {
            // First scan physical volumes
            VirtualDisk disk = _disks.get(i);
            String diskId = getDiskId(i);
            if (PartitionTable.isPartitioned(disk.getContent())) {
                for (PartitionTable table : PartitionTable.getPartitionTables(disk)) {
                    for (PartitionInfo part : table.getPartitions()) {
                        PhysicalVolumeInfo pvi = new PhysicalVolumeInfo(diskId, disk, part);
                        result.put(pvi.getIdentity(), pvi);
                    }
                }
            } else {
                PhysicalVolumeInfo pvi = new PhysicalVolumeInfo(diskId, disk);
                result.put(pvi.getIdentity(), pvi);
            }
        }
        return result;
    }

    private String getDiskId(int ordinal) throws IOException {
        VirtualDisk disk = _disks.get(ordinal);
        if (disk.getIsPartitioned()) {
            UUID guid = disk.getPartitions().getDiskGuid();
            if (guid != UUID.fromString("")) {
                return "DG" + guid.toString(); // "B"
            }

        }

        int sig = disk.getSignature();
        if (sig != 0) {
            return "DS" + String.format("%8x", sig);
        }

        return "DO" + ordinal;
    }

}
