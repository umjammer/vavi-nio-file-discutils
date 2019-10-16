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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;


public enum UnixFilePermissions {
    /**
     * Standard Unix-style file system permissions.
     *
     * No permissions.
     */
    None(0),
    /**
     * Any user execute permission.
     */
    OthersExecute(0x001),
    /**
     * Any user write permission.
     */
    OthersWrite(0x002),
    /**
     * Any user read permission.
     */
    OthersRead(0x004),
    /**
     * Group execute permission.
     */
    GroupExecute(0x008),
    /**
     * Group write permission.
     */
    GroupWrite(0x010),
    /**
     * Group read permission.
     */
    GroupRead(0x020),
    /**
     * Owner execute permission.
     */
    OwnerExecute(0x040),
    /**
     * Owner write permission.
     */
    OwnerWrite(0x080),
    /**
     * Owner read permission.
     */
    OwnerRead(0x100),
    /**
     * Sticky bit (meaning ill-defined).
     */
    Sticky(0x200),
    /**
     * Set GUID on execute.
     */
    SetGroupId(0x400),
    /**
     * Set UID on execute.
     */
    SetUserId(0x800);

    private int value;

    public int getValue() {
        return value;
    }

    private UnixFilePermissions(int value) {
        this.value = value;
    }


    /**
     * Any user all permissions.
     */
    public static final EnumSet<UnixFilePermissions> OthersAll = EnumSet.of(OthersExecute, OthersWrite, OthersRead);

    /**
     * Group all permissions.
     */
    public static final EnumSet<UnixFilePermissions> GroupAll = EnumSet.of(GroupExecute, GroupWrite, GroupRead);

    /**
     * Owner all permissions.
     */
    public static final EnumSet<UnixFilePermissions> OwnerAll = EnumSet.of(OthersExecute, OthersWrite, OthersRead);

    public static EnumSet<UnixFilePermissions> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.getValue()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UnixFilePermissions.class)));
    }

    public static long valueOf(EnumSet<UnixFilePermissions> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.getValue())).getSum();
    }
}
