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

package DiscUtils.Iso9660.RockRidge;

import DiscUtils.Iso9660.IsoUtilities;
import DiscUtils.Iso9660.SystemUseEntry;

public final class PosixFileInfoSystemUseEntry extends SystemUseEntry {
    public int FileMode;

    public int GroupId;

    public int Inode;

    public int NumLinks;

    public int UserId;

    public PosixFileInfoSystemUseEntry(String name, byte length, byte version, byte[] data, int offset) {
        checkAndSetCommonProperties(name, length, version, (byte) 36, (byte) 1);
        FileMode = IsoUtilities.toUInt32FromBoth(data, offset + 4);
        NumLinks = IsoUtilities.toUInt32FromBoth(data, offset + 12);
        UserId = IsoUtilities.toUInt32FromBoth(data, offset + 20);
        GroupId = IsoUtilities.toUInt32FromBoth(data, offset + 28);
        Inode = 0;
        if (length >= 44) {
            Inode = IsoUtilities.toUInt32FromBoth(data, offset + 36);
        }
    }
}