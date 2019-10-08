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

public enum ValueFlags {
    Named,
    Unknown0002,
    Unknown0004,
    Unknown0008,
    Unknown0010,
    Unknown0020,
    Unknown0040,
    Unknown0080,
    Unknown0100,
    Unknown0200,
    Unknown0400,
    Unknown0800,
    Unknown1000,
    Unknown2000,
    Unknown4000,
    Unknown8000;

    public static EnumSet<ValueFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ValueFlags.class)));
    }

    public static long valueOf(EnumSet<ValueFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }
}
