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

import java.util.UUID;

import DiscUtils.Core.Partitions.GuidPartitionInfo;
import DiscUtils.Core.Partitions.PartitionInfo;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SparseStreamOpenDelegate;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.Ownership;


/**
 * Information about a physical disk volume, which may be a partition or an
 * entire disk.
 */
public final class PhysicalVolumeInfo extends VolumeInfo {
    private static final UUID EMPTY = new UUID(0L, 0L);

    private final VirtualDisk _disk;

    private final String _diskId;

    private final SparseStreamOpenDelegate _streamOpener;

    /**
     * Initializes a new instance of the PhysicalVolumeInfo class.
     *
     * @param diskId The containing disk's identity.
     * @param disk The disk containing the partition.
     * @param partitionInfo Information about the partition.Use this constructor
     *            to represent a (BIOS or GPT) partition.
     */
    PhysicalVolumeInfo(String diskId, VirtualDisk disk, PartitionInfo partitionInfo) {
        _diskId = diskId;
        _disk = disk;
        _streamOpener = partitionInfo::open;
        _volumeType = partitionInfo.getVolumeType();
        _partition = partitionInfo;
    }

    /**
     * Initializes a new instance of the PhysicalVolumeInfo class.
     *
     * @param diskId The identity of the disk.
     * @param disk The disk itself.Use this constructor to represent an entire
     *            disk as a single volume.
     */
    PhysicalVolumeInfo(String diskId, VirtualDisk disk) {
        _diskId = diskId;
        _disk = disk;
        _streamOpener = () -> new SubStream(disk.getContent(), Ownership.None, 0, disk.getCapacity());
        _volumeType = PhysicalVolumeType.EntireDisk;
    }

    /**
     * Gets the disk geometry of the underlying storage medium (as used in BIOS
     * calls), may be null.
     */
    public Geometry getBiosGeometry() {
        return _disk.getBiosGeometry();
    }

    /**
     * Gets the one-byte BIOS type for this volume, which indicates the content.
     */
    public byte getBiosType() {
        return _partition == null ? (byte) 0 : _partition.getBiosType();
    }

    /**
     * Gets the unique identity of the disk containing the volume, if known.
     */
    public UUID getDiskIdentity() {
        return _volumeType != PhysicalVolumeType.EntireDisk ? _disk.getPartitions().getDiskGuid() : EMPTY;
    }

    /**
     * Gets the signature of the disk containing the volume (only valid for
     * partition-type volumes).
     */
    public int getDiskSignature() {
        return _volumeType != PhysicalVolumeType.EntireDisk ? _disk.getSignature() : 0;
    }

    /**
     * Gets the stable identity for this physical volume.
     * <p>
     * The stability of the identity depends the disk structure. In some cases
     * the identity may include a simple index, when no other information is
     * available. Best practice is to add disks to the Volume Manager in a
     * stable order, if the stability of this identity is paramount.
     */
    public String getIdentity() {
        if (_volumeType == PhysicalVolumeType.GptPartition) {
            return "VPG" + String.format("{%s}", getPartitionIdentity());
        }
        String partId;
        switch (_volumeType) {
        case EntireDisk:
            partId = "PD";
            break;
        case BiosPartition:
        case ApplePartition:
            partId = "PO" + Long.toHexString(_partition.getFirstSector() * _disk.getSectorSize());
            break;
        default:
            partId = "P*";
            break;
        }

        return "VPD:" + _diskId + ":" + partId;
    }

    /**
     * Gets the size of the volume, in bytes.
     */
    public long getLength() {
        return _partition == null ? _disk.getCapacity() : _partition.getSectorCount() * _disk.getSectorSize();
    }

    private PartitionInfo _partition;

    /**
     * Gets the underlying partition (if any).
     */
    public PartitionInfo getPartition() {
        return _partition;
    }

    /**
     * Gets the unique identity of the physical partition, if known.
     */
    public UUID getPartitionIdentity() {
        if (GuidPartitionInfo.class.isInstance(_partition)) {
            return GuidPartitionInfo.class.cast(_partition).getIdentity();
        }

        return EMPTY;
    }

    /**
     * Gets the disk geometry of the underlying storage medium, if any (may be
     * null).
     */
    public Geometry getPhysicalGeometry() {
        return _disk.getGeometry();
    }

    /**
     * Gets the offset of this volume in the underlying storage medium, if any
     * (may be Zero).
     */
    public long getPhysicalStartSector() {
        return _volumeType == PhysicalVolumeType.EntireDisk ? 0 : _partition.getFirstSector();
    }

    /**
     * Gets the type of the volume.
     */
    private PhysicalVolumeType _volumeType = PhysicalVolumeType.None;

    public PhysicalVolumeType getVolumeType() {
        return _volumeType;
    }

    /**
     * Opens the volume, providing access to its contents.
     *
     * @return A stream that can be used to access the volume.
     */
    public SparseStream open() {
        return _streamOpener.invoke();
    }
}
