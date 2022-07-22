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

/**
 * Information about a type of virtual disk.
 */
public final class VirtualDiskTypeInfo {

    /**
     * Gets or sets the algorithm for determining the geometry for a given disk
     * capacity.
     */
    private GeometryCalculation calcGeometry;

    public GeometryCalculation getCalcGeometry() {
        return calcGeometry;
    }

    public void setCalcGeometry(GeometryCalculation value) {
        calcGeometry = value;
    }

    /**
     * Gets or sets a value indicating whether this disk type can represent hard
     * disks.
     */
    private boolean canBeHardDisk;

    public boolean getCanBeHardDisk() {
        return canBeHardDisk;
    }

    public void setCanBeHardDisk(boolean value) {
        canBeHardDisk = value;
    }

    /**
     * Gets or sets a value indicating whether this disk type requires a
     * specific geometry for any given disk capacity.
     */
    private boolean deterministicGeometry;

    public boolean getDeterministicGeometry() {
        return deterministicGeometry;
    }

    public void setDeterministicGeometry(boolean value) {
        deterministicGeometry = value;
    }

    /**
     * Gets or sets the name of the virtual disk type.
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    /**
     * Gets or sets a value indicating whether this disk type persists the BIOS
     * geometry.
     */
    private boolean preservesBiosGeometry;

    public boolean getPreservesBiosGeometry() {
        return preservesBiosGeometry;
    }

    public void setPreservesBiosGeometry(boolean value) {
        preservesBiosGeometry = value;
    }

    /**
     * Gets or sets the variant of the virtual disk type.
     */
    private String variant;

    public String getVariant() {
        return variant;
    }

    public void setVariant(String value) {
        variant = value;
    }
}
