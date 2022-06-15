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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Flags indicating how an attribute's content is stored on disk.
 */
public enum AttributeFlags {
    /**
     * The data is stored in linear form.
     */
//    None,
    /**
     * The data is compressed.
     */
    Compressed,
    _dummy_0002,
    _dummy_0004,
    _dummy_0008,
    _dummy_0010,
    _dummy_0020,
    _dummy_0040,
    _dummy_0080,
    _dummy_0100,
    _dummy_0200,
    _dummy_0400,
    _dummy_0800,
    _dummy_1000,
    _dummy_2000,
    /**
     * The data is encrypted.
     */
    Encrypted,
    /**
     * The data is stored in sparse form.
     */
    Sparse;

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<AttributeFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AttributeFlags.class)));
    }
}
