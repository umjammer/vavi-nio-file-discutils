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

package discUtils.iso9660;

import java.util.List;

import discUtils.core.vfs.VfsContext;
import discUtils.iso9660.susp.SuspExtension;
import dotnet4j.io.Stream;


public class IsoContext extends VfsContext {

    private Stream dataStream;

    public Stream getDataStream() {
        return dataStream;
    }

    public void setDataStream(Stream value) {
        dataStream = value;
    }

    private String rockRidgeIdentifier;

    public String getRockRidgeIdentifier() {
        return rockRidgeIdentifier;
    }

    public void setRockRidgeIdentifier(String value) {
        rockRidgeIdentifier = value;
    }

    private boolean suspDetected;

    public boolean getSuspDetected() {
        return suspDetected;
    }

    public void setSuspDetected(boolean value) {
        suspDetected = value;
    }

    private List<SuspExtension> suspExtensions;

    public List<SuspExtension> getSuspExtensions() {
        return suspExtensions;
    }

    public void setSuspExtensions(List<SuspExtension> value) {
        suspExtensions = value;
    }

    private int suspSkipBytes;

    public int getSuspSkipBytes() {
        return suspSkipBytes;
    }

    public void setSuspSkipBytes(int value) {
        suspSkipBytes = value;
    }

    private CommonVolumeDescriptor volumeDescriptor;

    public CommonVolumeDescriptor getVolumeDescriptor() {
        return volumeDescriptor;
    }

    public void setVolumeDescriptor(CommonVolumeDescriptor value) {
        volumeDescriptor = value;
    }
}
