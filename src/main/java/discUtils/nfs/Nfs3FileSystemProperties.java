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

package discUtils.nfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;


/**
 * The properties a file system reports in FSINFO, a bit mask.
 *
 * @see "RFC 1813, 3.3.19 Procedure 19: FSINFO"
 */
public enum Nfs3FileSystemProperties {
    /** the file system supports hard links */
    HardLinks(0x0001),
    /** the file system supports symbolic links */
    SymbolicLinks(0x0002),
    /** PATHCONF is the same for every file, so it need only be asked for once */
    Homogeneous(0x0008),
    /** the server can set the times of a file */
    SetTime(0x0010);

    private final int value;

    Nfs3FileSystemProperties(int value) {
        this.value = value;
    }

    /** the bit of this property in the mask */
    public int getValue() {
        return value;
    }

    /** Unpacks a mask, ignoring bits this implementation has no name for. */
    public static EnumSet<Nfs3FileSystemProperties> valueOf(int value) {
        return Arrays.stream(values())
                .filter(property -> (value & property.value) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Nfs3FileSystemProperties.class)));
    }

    /** Packs properties back into a mask. */
    public static int valueOf(EnumSet<Nfs3FileSystemProperties> properties) {
        return properties.stream().mapToInt(Nfs3FileSystemProperties::getValue).reduce(0, (a, b) -> a | b);
    }
}
