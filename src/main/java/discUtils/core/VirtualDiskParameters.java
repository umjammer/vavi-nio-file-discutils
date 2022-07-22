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

import java.util.Map;

/**
 * common parameters for virtual disks.
 *
 * Not all attributes make sense for all kinds of disks, so some
 * may be null.  Modifying instances of this class does not modify the
 * disk itself.
 */
public class VirtualDiskParameters {

    /**
     * the type of disk adapter.
     */
    public GenericDiskAdapterType adapterType;

    /**
     * the logical (aka BIOS) geometry of the disk.
     */
    public Geometry biosGeometry;

    /**
     * the disk capacity.
     */
    public long capacity;

    /**
     * the type of disk (optical, hard disk, etc).
     */
    public VirtualDiskClass diskType;

    /**
     * a dictionary of extended parameters, that varies by disk type.
     */
    public Map<String, String> extendedParameters;

    /**
     * the physical (aka IDE) geometry of the disk.
     */
    public Geometry geometry;

    public GenericDiskAdapterType getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(GenericDiskAdapterType adapterType) {
        this.adapterType = adapterType;
    }

    public Geometry getBiosGeometry() {
        return biosGeometry;
    }

    public void setBiosGeometry(Geometry biosGeometry) {
        this.biosGeometry = biosGeometry;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public VirtualDiskClass getDiskType() {
        return diskType;
    }

    public void setDiskType(VirtualDiskClass diskType) {
        this.diskType = diskType;
    }

    public Map<String, String> getExtendedParameters() {
        return extendedParameters;
    }

    public void setExtendedParameters(Map<String, String> extendedParameters) {
        this.extendedParameters = extendedParameters;
    }

    public Geometry getGeometry() {
        return geometry;
    }
}