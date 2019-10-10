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

package DiscUtils.Btrfs;

import DiscUtils.Core.DiscFileSystemOptions;


public class BtrfsFileSystemOptions extends DiscFileSystemOptions {
    private long _subvolumeId;

    public BtrfsFileSystemOptions() {
        setUseDefaultSubvolume(true);
    }

    public long getSubvolumeId() {
        return _subvolumeId;
    }

    public void setSubvolumeId(long value) {
        _subvolumeId = value;
        setUseDefaultSubvolume(false);
    }

    private boolean __VerifyChecksums;

    public boolean getVerifyChecksums() {
        return __VerifyChecksums;
    }

    public void setVerifyChecksums(boolean value) {
        __VerifyChecksums = value;
    }

    private boolean __UseDefaultSubvolume;

    public boolean getUseDefaultSubvolume() {
        return __UseDefaultSubvolume;
    }

    public void setUseDefaultSubvolume(boolean value) {
        __UseDefaultSubvolume = value;
    }
}
