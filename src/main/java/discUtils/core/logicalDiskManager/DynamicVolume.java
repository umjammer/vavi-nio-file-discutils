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

package discUtils.core.logicalDiskManager;

import java.util.UUID;

import discUtils.core.LogicalVolumeStatus;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Sizes;


public class DynamicVolume {
    private final DynamicDiskGroup _group;

    public DynamicVolume(DynamicDiskGroup group, UUID volumeId) {
        _group = group;
        _identity = volumeId;
    }

    public byte getBiosType() {
        return getRecord().BiosType;
    }

    private UUID _identity;

    public UUID getIdentity() {
        return _identity;
    }

    public long getLength() {
        return getRecord().Size * Sizes.Sector;
    }

    private VolumeRecord getRecord() {
        return _group.getVolume(getIdentity());
    }

    public LogicalVolumeStatus getStatus() {
        return _group.getVolumeStatus(getRecord().Id);
    }

    public SparseStream open() {
        if (getStatus() == LogicalVolumeStatus.Failed) {
            throw new dotnet4j.io.IOException("Attempt to open 'failed' volume");
        }

        return _group.openVolume(getRecord().Id);
    }
}
