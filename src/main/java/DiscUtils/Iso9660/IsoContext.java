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

package DiscUtils.Iso9660;

import java.util.List;

import DiscUtils.Core.Vfs.VfsContext;
import moe.yo3explorer.dotnetio4j.Stream;


public class IsoContext extends VfsContext {
    private Stream __DataStream;

    public Stream getDataStream() {
        return __DataStream;
    }

    public void setDataStream(Stream value) {
        __DataStream = value;
    }

    private String __RockRidgeIdentifier;

    public String getRockRidgeIdentifier() {
        return __RockRidgeIdentifier;
    }

    public void setRockRidgeIdentifier(String value) {
        __RockRidgeIdentifier = value;
    }

    private boolean __SuspDetected;

    public boolean getSuspDetected() {
        return __SuspDetected;
    }

    public void setSuspDetected(boolean value) {
        __SuspDetected = value;
    }

    private List<SuspExtension> __SuspExtensions;

    public List<SuspExtension> getSuspExtensions() {
        return __SuspExtensions;
    }

    public void setSuspExtensions(List<SuspExtension> value) {
        __SuspExtensions = value;
    }

    private int __SuspSkipBytes;

    public int getSuspSkipBytes() {
        return __SuspSkipBytes;
    }

    public void setSuspSkipBytes(int value) {
        __SuspSkipBytes = value;
    }

    private CommonVolumeDescriptor __VolumeDescriptor;

    public CommonVolumeDescriptor getVolumeDescriptor() {
        return __VolumeDescriptor;
    }

    public void setVolumeDescriptor(CommonVolumeDescriptor value) {
        __VolumeDescriptor = value;
    }
}
