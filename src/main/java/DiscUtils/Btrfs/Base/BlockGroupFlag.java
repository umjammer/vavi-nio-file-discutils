//
// Copyright (c) 2017, Bianco Veigel
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

package DiscUtils.Btrfs.Base;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public enum BlockGroupFlag {
    /**
     * The type of storage this block group offers. SYSTEM chunks cannot be
     * mixed, but DATA and METADATA chunks can be mixed.
     */
    Data,
    /**
     * The type of storage this block group offers. SYSTEM chunks cannot be
     * mixed, but DATA and METADATA chunks can be mixed.
     */
    System,
    /**
     * The type of storage this block group offers. SYSTEM chunks cannot be
     * mixed, but DATA and METADATA chunks can be mixed.
     */
    Metadata,
    /**
     * Striping
     */
    Raid0,
    /**
     * Mirror on a separate device
     */
    Raid1,
    /**
     * Mirror on a single device
     */
    Dup,
    /**
     * Striping and mirroring
     */
    Raid10,
    /**
     * Parity striping with single-disk fault tolerance
     */
    Raid5,
    /**
     * Parity striping with double-disk fault tolerance
     */
    Raid6;

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<BlockGroupFlag> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(BlockGroupFlag.class)));
    }
}
