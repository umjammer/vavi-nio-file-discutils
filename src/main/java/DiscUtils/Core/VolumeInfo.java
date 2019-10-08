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

import java.io.Serializable;

import DiscUtils.Streams.SparseStream;


/**
 * Base class that holds information about a disk volume.
 */
public abstract class VolumeInfo implements Serializable {
    public VolumeInfo() {
    }

    /**
     * Gets the one-byte BIOS type for this volume, which indicates the content.
     */
    public abstract byte getBiosType();

    /**
     * Gets the size of the volume, in bytes.
     */
    public abstract long getLength();

    /**
     * Gets the stable volume identity.
     * The stability of the identity depends the disk structure.
     * In some cases the identity may include a simple index, when no other
     * information
     * is available. Best practice is to add disks to the Volume Manager in a
     * stable
     * order, if the stability of this identity is paramount.
     */
    public abstract String getIdentity();

    /**
     * Gets the disk geometry of the underlying storage medium, if any (may be
     * null).
     */
    public abstract Geometry getPhysicalGeometry();

    /**
     * Gets the disk geometry of the underlying storage medium (as used in BIOS
     * calls), may be null.
     */
    public abstract Geometry getBiosGeometry();

    /**
     * Gets the offset of this volume in the underlying storage medium, if any
     * (may be Zero).
     */
    public abstract long getPhysicalStartSector();

    /**
     * Opens the volume, providing access to it's contents.
     *
     * @return Stream that can access the volume's contents.
     */
    public abstract SparseStream open();
}
