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

package DiscUtils.Fat;

import java.util.Arrays;


/**
 * Enumeration of known FAT types.
 */
public enum FatType {
    /**
     * Represents no known FAT type.
     */
    None(0, "Unknown FAT"),
    /**
     * Represents a 12-bit FAT.
     */
    Fat12(12, "Microsoft FAT12"),
    /**
     * Represents a 16-bit FAT.
     */
    Fat16(16, "Microsoft FAT16"),
    /**
     * Represents a 32-bit FAT.
     */
    Fat32(32, "Microsoft FAT32");

    private int value;
    private String friendlyName;

    public int getValue() {
        return value;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    private FatType(int value, String friendlyName) {
        this.value = value;
        this.friendlyName = friendlyName;
    }

    public static FatType valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.getValue() == value).findFirst().get();
    }
}
