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
 * Class whose instances represent a CHS (Cylinder, Head, Sector) address on a
 * disk.
 * Instances of this class are immutable.
 */
public final class ChsAddress {
    /**
     * The address of the first sector on any disk.
     */
    public static final ChsAddress First = new ChsAddress(0, 0, 1);

    /**
     * Initializes a new instance of the ChsAddress class.
     * 
     * @param cylinder The number of cylinders of the disk.
     * @param head The number of heads (aka platters) of the disk.
     * @param sector The number of sectors per track/cylinder of the disk.
     */
    public ChsAddress(int cylinder, int head, int sector) {
        __Cylinder = cylinder;
        __Head = head;
        __Sector = sector;
    }

    /**
     * Gets the cylinder number (zero-based).
     */
    private int __Cylinder;

    public int getCylinder() {
        return __Cylinder;
    }

    /**
     * Gets the head (zero-based).
     */
    private int __Head;

    public int getHead() {
        return __Head;
    }

    /**
     * Gets the sector number (one-based).
     */
    private int __Sector;

    public int getSector() {
        return __Sector;
    }

    /**
     * Determines if this object is equivalent to another.
     * 
     * @param obj The object to test against.
     * @return
     *         {@code true}
     *         if the
     *         {@code obj}
     *         is equivalent, else
     *         {@code false}
     *         .
     */
    public boolean equals(Object obj) {
        try {
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }

            ChsAddress other = (ChsAddress) obj;
            return getCylinder() == other.getCylinder() && getHead() == other.getHead() && getSector() == other.getSector();
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    /**
     * Calculates the hash code for this object.
     * 
     * @return The hash code.
     */
    public int hashCode() {
        return getCylinder() ^ getHead() ^ getSector();
    }

    /**
     * Gets a string representation of this object, in the form (C/H/S).
     * 
     * @return The string representation.
     */
    public String toString() {
        return "(" + getCylinder() + "/" + getHead() + "/" + getSector() + ")";
    }

}
