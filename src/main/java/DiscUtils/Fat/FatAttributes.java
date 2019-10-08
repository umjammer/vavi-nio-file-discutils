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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

public enum FatAttributes {
    __dummyEnum__0,
    ReadOnly,
    Hidden,
    __dummyEnum__1,
    System,
    __dummyEnum__2,
    __dummyEnum__3,
    __dummyEnum__4,
    VolumeId,
    __dummyEnum__5,
    __dummyEnum__6,
    __dummyEnum__7,
    __dummyEnum__8,
    __dummyEnum__9,
    __dummyEnum__10,
    __dummyEnum__11,
    Directory,
    __dummyEnum__12,
    __dummyEnum__13,
    __dummyEnum__14,
    __dummyEnum__15,
    __dummyEnum__16,
    __dummyEnum__17,
    __dummyEnum__18,
    __dummyEnum__19,
    __dummyEnum__20,
    __dummyEnum__21,
    __dummyEnum__22,
    __dummyEnum__23,
    __dummyEnum__24,
    __dummyEnum__25,
    __dummyEnum__26,
    Archive;

    public static EnumSet<FatAttributes> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.ordinal()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FatAttributes.class)));
    }

    public static long valueOf(EnumSet<FatAttributes> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.ordinal())).getSum();
    }

    public static Map<String, Object> convert(EnumSet<FatAttributes> flags) {
        return Collections.EMPTY_MAP; // TODO
    }

    public static EnumSet<FatAttributes> convert(Map<String, Object> value) {
        return EnumSet.noneOf(FatAttributes.class); // TODO
    }
}