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

import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import DiscUtils.Core.CoreCompat.EnumSettable;

public enum NtfsFileAttributes implements EnumSettable {
    /**
     * File attributes as stored natively by NTFS.
     *
     * No attributes.
     */
    None(0x00000000),
    /**
     * The file is read-only.
     */
    ReadOnly(0x00000001),
    /**
     * The file is hidden.
     */
    Hidden(0x00000002),
    /**
     * The file is part of the Operating System.
     */
    System(0x00000004),
    /**
     * The file should be archived.
     */
    Archive(0x00000020),
    /**
     * The file is actually a device.
     */
    Device(0x00000040),
    /**
     * The file is a 'normal' file.
     */
    Normal(0x00000080),
    /**
     * The file is a temporary file.
     */
    Temporary(0x00000100),
    /**
     * The file content is stored in sparse form.
     */
    Sparse(0x00000200),
    /**
     * The file has a reparse point attached.
     */
    ReparsePoint(0x00000400),
    /**
     * The file content is stored compressed.
     */
    Compressed(0x00000800),
    /**
     * The file is an 'offline' file.
     */
    Offline(0x00001000),
    /**
     * The file is not indexed.
     */
    NotIndexed(0x00002000),
    /**
     * The file content is encrypted.
     */
    Encrypted(0x00004000),
    /**
     * The file is actually a directory.
     */
    Directory(0x10000000),
    /**
     * The file has an index attribute.
     */
    IndexView(0x20000000);
    int value;
    public int getValue() {
        return value;
    }
    NtfsFileAttributes(int value) {
        this.value = value;
    }

    public Supplier<Integer> supplier() { return this::getValue; }

    public Function<Integer, Boolean> function() { return v -> (v & supplier().get()) != 0; };

    public static long valueOf(EnumSet<NtfsFileAttributes> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
