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

package DiscUtils.Nfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;


public enum Nfs3AccessPermissions {
    None(0x00),
    Read(0x01),
    Lookup(0x02),
    Modify(0x04),
    Extend(0x08),
    Delete(0x10),
    Execute(0x20);

    public static final EnumSet<Nfs3AccessPermissions> All = EnumSet.of(Read, Lookup, Modify, Extend, Delete, Execute);

    private int value;

    public int getValue() {
        return value;
    }

    private Nfs3AccessPermissions(int value) {
        this.value = value;
    }

    public static EnumSet<Nfs3AccessPermissions> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.getValue()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Nfs3AccessPermissions.class)));
    }

    public static long valueOf(EnumSet<Nfs3AccessPermissions> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.getValue())).getSum();
    }
}
