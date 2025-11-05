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

package discUtils.core;

import java.util.UUID;

import discUtils.core.partitions.GuidPartitionInfo;
import discUtils.core.partitions.PartitionInfo;
import discUtils.streams.SparseStream;
import discUtils.streams.SparseStreamOpenDelegate;
import discUtils.streams.SubStream;
import discUtils.streams.util.Ownership;


/**
 * Information about a physical disk volume, which may be a partition or an
 * entire disk.
 */
public final class PhysicalVolumeInfo extends VolumeInfo {

    private static final UUID EMPTY = new UUID(0L, 0L);

    private final VirtualDisk disk;

    private final String diskId;

    private final SparseStreamOpenDelegate streamOpener;

    /**
     * Initializes a new instance of the PhysicalVolumeInfo class.
     *
     * @param diskId The containing disk's identity.
     * @param disk The disk containing the partition.
     * @param partitionInfo Information about the partition.Use this constructor
     *            to represent a (BIOS or GPT) partition.
     */
    PhysicalVolumeInfo(String diskId, VirtualDisk disk, PartitionInfo partitionInfo) {
        this.diskId = diskId;
        this.disk = disk;
        streamOpener = partitionInfo::open;
        volumeType = partitionInfo.getVolumeType();
        partition = partitionInfo;
    }

    /**
     * Initializes a new instance of the PhysicalVolumeInfo class.
     *
     * @param diskId The identity of the disk.
     * @param disk The disk itself.Use this constructor to represent an entire
     *            disk as a single volume.
     */
    PhysicalVolumeInfo(String diskId, VirtualDisk disk) {
        this.diskId = diskId;
        this.disk = disk;
        streamOpener = () -> new SubStream(disk.getContent(), Ownership.None, 0, disk.getCapacity());
        volumeType = PhysicalVolumeType.EntireDisk;
    }

    /**
     * Gets the disk geometry of the underlying storage medium (as used in BIOS
     * calls), may be null.
     */
    @Override public Geometry getBiosGeometry() {
        return disk.getBiosGeometry();
    }

    /**
     * Gets the one-byte BIOS type for this volume, which indicates the content.
     */
    @Override public byte getBiosType() {
        return partition == null ? (byte) 0 : partition.getBiosType();
    }

    /**
     * Gets the unique identity of the disk containing the volume, if known.
     */
    public UUID getDiskIdentity() {
        return volumeType != PhysicalVolumeType.EntireDisk ? disk.getPartitions().getDiskGuid() : EMPTY;
    }

    /**
     * Gets the signature of the disk containing the volume (only valid for
     * partition-type volumes).
     */
    public int getDiskSignature() {
        return volumeType != PhysicalVolumeType.EntireDisk ? disk.getSignature() : 0;
    }

    /**
     * Gets the stable identity for this physical volume.
     * <p>
     * The stability of the identity depends the disk structure. In some cases
     * the identity may include a simple index, when no other information is
     * available. Best practice is to add disks to the Volume Manager in a
     * stable order, if the stability of this identity is paramount.
     */
    @Override public String getIdentity() {
        if (volumeType == PhysicalVolumeType.GptPartition) {
            return "VPG" + "{%s}".formatted(getPartitionIdentity());
        }
        String partId = switch (volumeType) {
            case EntireDisk -> "PD";
            case BiosPartition, ApplePartition ->
                    "PO" + Long.toHexString(partition.getFirstSector() * disk.getSectorSize());
            default -> "P*";
        };

        return "VPD:" + diskId + ":" + partId;
    }

    /**
     * Gets the size of the volume, in bytes.
     */
    @Override public long getLength() {
        return partition == null ? disk.getCapacity() : partition.getSectorCount() * disk.getSectorSize();
    }

    private PartitionInfo partition;

    /**
     * Gets the underlying partition (if any).
     */
    public PartitionInfo getPartition() {
        return partition;
    }

    /**
     * Gets the unique identity of the physical partition, if known.
     */
    public UUID getPartitionIdentity() {
        if (partition instanceof GuidPartitionInfo) {
            return ((GuidPartitionInfo) partition).getIdentity();
        }

        return EMPTY;
    }

    /**
     * Gets the disk geometry of the underlying storage medium, if any (may be
     * null).
     */
    @Override public Geometry getPhysicalGeometry() {
        return disk.getGeometry();
    }

    /**
     * Gets the offset of this volume in the underlying storage medium, if any
     * (may be Zero).
     */
    @Override public long getPhysicalStartSector() {
        return volumeType == PhysicalVolumeType.EntireDisk ? 0 : partition.getFirstSector();
    }

    /**
     * Gets the type of the volume.
     */
    private PhysicalVolumeType volumeType;

    public PhysicalVolumeType getVolumeType() {
        return volumeType;
    }

    /**
     * Opens the volume, providing access to its contents.
     *
     * @return A stream that can be used to access the volume.
     */
    @Override public SparseStream open() {
        return streamOpener.invoke();
    }

    @Override
    public String toString() {
        return getIdentity();
    }
}
