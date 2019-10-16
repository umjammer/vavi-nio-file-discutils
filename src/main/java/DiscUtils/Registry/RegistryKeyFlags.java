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

package DiscUtils.Registry;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public enum RegistryKeyFlags {
    /**
     * The per-key flags present on registry keys.
     *
     * Unknown purpose.
     */
    Unknown0001(0x0001),
    /**
     * Unknown purpose.
     */
    Unknown0002(0x0002),
    /**
     * The key is the root key in the registry hive.
     */
    Root(0x0004),
    /**
     * Unknown purpose.
     */
    Unknown0008(0x0008),
    /**
     * The key is a link to another key.
     */
    Link(0x0010),
    /**
     * This is a normal key.
     */
    Normal(0x0020),
    /**
     * Unknown purpose.
     */
    Unknown0040(0x0040),
    /**
     * Unknown purpose.
     */
    Unknown0080(0x0080),
    /**
     * Unknown purpose.
     */
    Unknown0100(0x0100),
    /**
     * Unknown purpose.
     */
    Unknown0200(0x0200),
    /**
     * Unknown purpose.
     */
    Unknown0400(0x0400),
    /**
     * Unknown purpose.
     */
    Unknown0800(0x0800),
    /**
     * Unknown purpose.
     */
    Unknown1000(0x1000),
    /**
     * Unknown purpose.
     */
    Unknown2000(0x2000),
    /**
     * Unknown purpose.
     */
    Unknown4000(0x4000),
    /**
     * Unknown purpose.
     */
    Unknown8000(0x8000);

    private int value;

    public int getValue() {
        return value;
    }

    private RegistryKeyFlags(int value) {
        this.value = value;
    }

    public static EnumSet<RegistryKeyFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.getValue()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RegistryKeyFlags.class)));
    }

    public static long valueOf(EnumSet<RegistryKeyFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.getValue())).getSum();
    }
}
