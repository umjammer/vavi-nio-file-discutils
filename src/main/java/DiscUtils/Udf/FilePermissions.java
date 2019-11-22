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

package DiscUtils.Udf;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Standard Unix-style file system permissions.
 */
enum FilePermissions {
    /**
     * No permissions.
     */
//    None,
    /**
     * Any user execute permission.
     */
    OthersExecute,
    /**
     * Any user write permission.
     */
    OthersWrite,
    /**
     * Any user read permission.
     */
    OthersRead,
    /**
     * Any user change attributes permission.
     */
    OthersChangeAttributes,
    /**
     * Any user delete permission.
     */
    OthersDelete,
    /**
     * Group execute permission.
     */
    GroupExecute,
    /**
     * Group write permission.
     */
    GroupWrite,
    /**
     * Group read permission.
     */
    GroupRead,
    /**
     * Group change attributes permission.
     */
    GroupChangeAttributes,
    /**
     * Group delete permission.
     */
    GroupDelete,
    /**
     * Owner execute permission.
     */
    OwnerExecute,
    /**
     * Owner write permission.
     */
    OwnerWrite,
    /**
     * Owner read permission.
     */
    OwnerRead,
    /**
     * Owner change attributes permission.
     */
    OwnerChangeAttributes,
    /**
     * Owner delete permission.
     */
    OwnerDelete;

    private int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    };

    public static EnumSet<FilePermissions> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FilePermissions.class)));
    }

    public static long valueOf(EnumSet<FilePermissions> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
