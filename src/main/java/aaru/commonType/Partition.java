//
// Aaru Data Preservation Suite
//
//
// Filename       : Partition.cs
// Author(s)      : Natalia Portillo <claunia@claunia.com>
//
// Component      : Aaru common types.
//
// Description
//
//     Contains common partition types.
//
// License
//
//     Permission is hereby granted, free of charge, to any person obtaining a
//     copy of this software and associated documentation files (the
//     "Software"), to deal in the Software without restriction, including
//     without limitation the rights to use, copy, modify, merge, publish,
//     distribute, sublicense, and/or sell copies of the Software, and to
//     permit persons to whom the Software is furnished to do so, subject to
//     the following conditions:
//
//     The above copyright notice and this permission notice shall be included
//     in all copies or substantial portions of the Software.
//
//     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
//     OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//     MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//     IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//     CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//     TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//     SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
//
// Copyright © 2011-2021 Natalia Portillo
//

package aaru.commonType;


/** Partition structure. */
public class Partition implements Comparable<Partition> {

    /** Partition number, 0-started */
    public long sequence;
    /** Partition type */
    public String type;
    /** Partition name (if the scheme supports it) */
    public String name;
    /** Start of the partition, in bytes */
    public long offset;
    /** LBA of partition start */
    public long start;
    /** Length in bytes of the partition */
    public long size;
    /** Length in sectors of the partition */
    public long length;
    /** Information that does not find space in this struct */
    public String description;

    /** LBA of last partition sector */
    public final long end() {
        return start + length - 1;
    }

    /** Name of partition scheme that contains this partition */
    public String scheme;

    /**
     * Compares two partitions
     *
     * @param other Partition to compare with
     * @return 0 if both partitions start and end at the same sector
     */
    private boolean equals(Partition other) {
        return start == other.start && length == other.length;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Partition && equals((Partition) obj);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(start) + Long.hashCode(end());
    }

    /**
     * Compares this partition with another and returns an integer that indicates whether the current partition
     * precedes, follows, or is in the same place as the other partition.
     *
     * @param other Partition to compare with
     * @return A value that indicates the relative equality of the partitions being compared.
     */
    public int compareTo(Partition other) {
        if (start == other.start &&
                end() == other.end())
            return 0;

        if (start > other.start ||
                end() > other.end())
            return 1;

        return -1;
    }

    // Define the equality operator.
    public static boolean operatorEquals(Partition operand1, Partition operand2) {
        return operand1.equals(operand2);
    }

    // Define the inequality operator.
    public static boolean operatorNotEquals(Partition operand1, Partition operand2) {
        return !operand1.equals(operand2);
    }

    // Define the is greater than operator.
    public static boolean operatorLT(Partition operand1, Partition operand2) {
        return operand1.compareTo(operand2) > 0;
    }

    // Define the is less than operator.
    public static boolean operatorGT(Partition operand1, Partition operand2) {
        return operand1.compareTo(operand2) < 0;
    }

    // Define the is greater than or equal to operator.
    public static boolean operatorLE(Partition operand1, Partition operand2) {
        return operand1.compareTo(operand2) >= 0;
    }

    // Define the is less than or equal to operator.
    public static boolean operatorGE(Partition operand1, Partition operand2) {
        return operand1.compareTo(operand2) <= 0;
    }
}
