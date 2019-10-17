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
import java.util.stream.Collectors;


public enum FatAttributes {
    ReadOnly(0x01),
    Hidden(0x02),
    System(0x04),
    VolumeId(0x08),
    Directory(0x10),
    Archive(0x20);

    private int value;

    public int getValue() {
        return value;
    }

    private FatAttributes(int value) {
        this.value = value;
    }

    public static EnumSet<FatAttributes> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.getValue()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FatAttributes.class)));
    }

    public static long valueOf(EnumSet<FatAttributes> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }

    // TODO name()
    public static Map<String, Object> toMap(EnumSet<FatAttributes> flags) {
        return flags.stream().collect(Collectors.toMap(f -> f.name(), f -> true));
    }

    // TODO name()
    public static EnumSet<FatAttributes> toEnumSet(Map<String, Object> flags) {
        return Arrays.stream(values())
                .filter(v -> Boolean.class.cast(flags.get(v.name())))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FatAttributes.class)));
    }
}
