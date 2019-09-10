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

/**
 * Information about a type of virtual disk.
 */
public final class VirtualDiskTypeInfo {
    /**
     * Gets or sets the algorithm for determining the geometry for a given disk
     * capacity.
     */
    private GeometryCalculation __CalcGeometry;

    public GeometryCalculation getCalcGeometry() {
        return __CalcGeometry;
    }

    public void setCalcGeometry(GeometryCalculation value) {
        __CalcGeometry = value;
    }

    /**
     * Gets or sets a value indicating whether this disk type can represent hard
     * disks.
     */
    private boolean __CanBeHardDisk;

    public boolean getCanBeHardDisk() {
        return __CanBeHardDisk;
    }

    public void setCanBeHardDisk(boolean value) {
        __CanBeHardDisk = value;
    }

    /**
     * Gets or sets a value indicating whether this disk type requires a
     * specific geometry for any given disk capacity.
     */
    private boolean __DeterministicGeometry;

    public boolean getDeterministicGeometry() {
        return __DeterministicGeometry;
    }

    public void setDeterministicGeometry(boolean value) {
        __DeterministicGeometry = value;
    }

    /**
     * Gets or sets the name of the virtual disk type.
     */
    private String __Name;

    public String getName() {
        return __Name;
    }

    public void setName(String value) {
        __Name = value;
    }

    /**
     * Gets or sets a value indicating whether this disk type persists the BIOS
     * geometry.
     */
    private boolean __PreservesBiosGeometry;

    public boolean getPreservesBiosGeometry() {
        return __PreservesBiosGeometry;
    }

    public void setPreservesBiosGeometry(boolean value) {
        __PreservesBiosGeometry = value;
    }

    /**
     * Gets or sets the variant of the virtual disk type.
     */
    private String __Variant;

    public String getVariant() {
        return __Variant;
    }

    public void setVariant(String value) {
        __Variant = value;
    }

}
