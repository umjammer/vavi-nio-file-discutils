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

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SparseStreamOpenDelegate;


/**
 * Information about a logical disk volume, which may be backed by one or more
 * physical volumes.
 */
public final class LogicalVolumeInfo extends VolumeInfo {
    private UUID _guid;

    private final SparseStreamOpenDelegate _opener;

    private final PhysicalVolumeInfo _physicalVol;

    public LogicalVolumeInfo(UUID guid,
            PhysicalVolumeInfo physicalVolume,
            SparseStreamOpenDelegate opener,
            long length,
            byte biosType,
            LogicalVolumeStatus status) {
        _guid = guid;
        _physicalVol = physicalVolume;
        _opener = opener;
        __Length = length;
        __BiosType = biosType;
        __Status = status;
    }

    /**
     * Gets the disk geometry of the underlying storage medium (as used in BIOS
     * calls), may be null.
     */
    public Geometry getBiosGeometry() {
        return _physicalVol == null ? Geometry.getNull() : _physicalVol.getBiosGeometry();
    }

    /**
     * Gets the one-byte BIOS type for this volume, which indicates the content.
     */
    private byte __BiosType;

    public byte getBiosType() {
        return __BiosType;
    }

    /**
     * The stable identity for this logical volume.
     * The stability of the identity depends the disk structure.
     * In some cases the identity may include a simple index, when no other
     * information
     * is available. Best practice is to add disks to the Volume Manager in a
     * stable
     * order, if the stability of this identity is paramount.
     */
    public String getIdentity() {
        if (_guid != UUID.fromString("")) {
            return "VLG" + _guid.toString(); // "B"
        }

        return "VLP:" + _physicalVol.getIdentity();
    }

    /**
     * Gets the length of the volume (in bytes).
     */
    private long __Length;

    public long getLength() {
        return __Length;
    }

    /**
     * Gets the disk geometry of the underlying storage medium, if any (may be
     * Geometry.Null).
     */
    public Geometry getPhysicalGeometry() {
        return _physicalVol == null ? Geometry.getNull() : _physicalVol.getPhysicalGeometry();
    }

    /**
     * Gets the offset of this volume in the underlying storage medium, if any
     * (may be Zero).
     */
    public long getPhysicalStartSector() {
        return _physicalVol == null ? 0 : _physicalVol.getPhysicalStartSector();
    }

    /**
     * Gets the status of the logical volume, indicating volume health.
     */
    private LogicalVolumeStatus __Status = LogicalVolumeStatus.Healthy;

    public LogicalVolumeStatus getStatus() {
        return __Status;
    }

    /**
     * Gets the underlying physical volume info
     */
    public PhysicalVolumeInfo getPhysicalVolume() {
        return _physicalVol;
    }

    /**
     * Opens a stream with access to the content of the logical volume.
     *
     * @return The volume's content as a stream.
     */
    public SparseStream open() {
        return _opener.invoke();
    }

}
