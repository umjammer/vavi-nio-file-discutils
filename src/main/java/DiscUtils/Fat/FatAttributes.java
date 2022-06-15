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
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public enum FatAttributes {
    ReadOnly,
    Hidden,
    System,
    VolumeId,
    Directory,
    Archive;

    // TODO
    public Supplier<Integer> supplier() {
        return () -> 1 << this.ordinal();
    }

    // TODO
    public Function<Integer, Boolean> function() {
        return v -> (v & supplier().get()) != 0;
    }

    public static EnumSet<FatAttributes> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FatAttributes.class)));
    }

    public static long valueOf(EnumSet<FatAttributes> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }

    // TODO name()
    public static Map<String, Object> toMap(EnumSet<FatAttributes> flags) {
        return flags.stream().collect(Collectors.toMap(Enum::name, f -> true));
    }

    // TODO name()
    public static EnumSet<FatAttributes> toEnumSet(Map<String, Object> flags) {
        return Arrays.stream(values())
                .filter(v -> flags.containsKey(v.name()) && (Boolean) flags.get(v.name()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FatAttributes.class)));
    }
}
