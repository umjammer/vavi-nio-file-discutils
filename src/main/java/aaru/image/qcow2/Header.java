//
// Aaru Data Preservation Suite
//
//
// Filename       : Structs.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Disk image plugins.
//
// Description
//
//     Contains structures for QEMU Copy-On-Write v2 disk images.
//
// License
//
//     This library is free software; you can redistribute it and/or modify
//     it under the terms of the GNU Lesser General Public License as
//     published by the Free Software Foundation; either version 2.1 of the
//     License, or (at your option) any later version.
//
//     This library is distributed in the hope that it will be useful, but
//     WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//     Lesser General Public License for more details.
//
//     You should have received a copy of the GNU Lesser General Public
//     License along with this library; if not, see <http://www.gnu.org/licenses/>.
//
//
// Copyright © 2011-2021 Natalia Portillo
//

package aaru.image.qcow2;

import vavi.util.serdes.Element;
import vavi.util.serdes.Serdes;


/** QCOW header, big-endian */
@Serdes
class Header {
    /** 
     * @see Constants#QCOW_MAGIC
     */
    @Element(sequence = 1)
    public int magic;
    /** Must be 1 */
    @Element(sequence = 2)
    public int version;
    /** Offset inside file to string containing backing file */
    @Element(sequence = 3)
    public long backingFileOffset;
    /** Size of {@link #backingFileOffset} */
    @Element(sequence = 4)
    public int backingFileSize;
    /** Cluster bits */
    @Element(sequence = 5)
    public int clusterBits;
    /** Size in bytes */
    @Element(sequence = 6)
    public long size;
    /** Encryption method */
    @Element(sequence = 7)
    public int cryptMethod;
    /** Size of L1 table */
    @Element(sequence = 8)
    public int l1Size;
    /** Offset to L1 table */
    @Element(sequence = 9)
    public long l1TableOffset;
    /** Offset to reference count table */
    @Element(sequence = 10)
    public long refCountTableOffset;
    /** How many clusters does the refcount table span */
    @Element(sequence = 11)
    public int refCountTableClusters;
    /** Number of snapshots */
    @Element(sequence = 12)
    public int snapshots;
    /** Offset to QCowSnapshotHeader */
    @Element(sequence = 13)
    public long snapshotsOffset;

    // Added in version 3
    @Element(sequence = 14)
    public long features;
    @Element(sequence = 15)
    public long compatFeatures;
    @Element(sequence = 16)
    public long autoClearFeatures;
    @Element(sequence = 17)
    public int refCountOrder;
    @Element(sequence = 18)
    public int headerLength;
}
