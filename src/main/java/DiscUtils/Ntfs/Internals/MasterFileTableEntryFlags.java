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

package DiscUtils.Ntfs.Internals;

import java.util.function.Function;
import java.util.function.Supplier;

import DiscUtils.Core.CoreCompat.EnumSettable;

public enum MasterFileTableEntryFlags implements EnumSettable {
    /**
     * Flags indicating the nature of a Master File Table entry.
     *
     * Default value.
     */
    None(0x0000),
    /**
     * The entry is currently in use.
     */
    InUse(0x0001),
    /**
     * The entry is for a directory (rather than a file).
     */
    IsDirectory(0x0002),
    /**
     * The entry is for a file that forms parts of the NTFS meta-data.
     */
    IsMetaFile(0x0004),
    /**
     * The entry contains index attributes.
     */
    HasViewIndex(0x0008);

    private int value;

    public int getValue() {
        return value;
    }

    private MasterFileTableEntryFlags(int value) {
        this.value = value;
    }

    public Supplier<Integer> supplier() {
        return this::getValue;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & supplier().get()) != 0;
    };
}
