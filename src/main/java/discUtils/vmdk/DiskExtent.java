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

package discUtils.vmdk;

import java.io.IOException;

import discUtils.core.FileLocator;
import discUtils.core.VirtualDiskExtent;
import discUtils.streams.MappedStream;
import discUtils.streams.SparseStream;
import discUtils.streams.ZeroStream;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


public final class DiskExtent extends VirtualDiskExtent {

    private FileAccess access;

    private final ExtentDescriptor descriptor;

    private final long diskOffset;

    private FileLocator fileLocator;

    private Stream monolithicStream;

    public DiskExtent(ExtentDescriptor descriptor, long diskOffset, FileLocator fileLocator, FileAccess access) {
        this.descriptor = descriptor;
        this.diskOffset = diskOffset;
        this.fileLocator = fileLocator;
        this.access = access;
    }

    public DiskExtent(ExtentDescriptor descriptor, long diskOffset, Stream monolithicStream) {
        this.descriptor = descriptor;
        this.diskOffset = diskOffset;
        this.monolithicStream = monolithicStream;
    }

    @Override
    public long getCapacity() {
        return descriptor.getSizeInSectors() * Sizes.Sector;
    }

    @Override
    public boolean isSparse() {
        return descriptor.getType() == ExtentType.Sparse || descriptor.getType() == ExtentType.VmfsSparse ||
               descriptor.getType() == ExtentType.Zero;
    }

    @Override
    public long getStoredSize() {
        if (monolithicStream != null) {
            return monolithicStream.getLength();
        }

        try (Stream s = fileLocator.open(descriptor.getFileName(), FileMode.Open, FileAccess.Read, FileShare.Read)) {
            return s.getLength();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public MappedStream openContent(SparseStream parent, Ownership ownsParent) throws IOException {
        FileAccess access = FileAccess.Read;
        FileShare share = FileShare.Read;
        if (descriptor.getAccess() == ExtentAccess.ReadWrite && this.access != FileAccess.Read) {
            access = FileAccess.ReadWrite;
            share = FileShare.None;
        }

        if (descriptor.getType() != ExtentType.Sparse && descriptor.getType() != ExtentType.VmfsSparse &&
            descriptor.getType() != ExtentType.Zero) {
            if (ownsParent == Ownership.Dispose && parent != null) {
                try {
                    parent.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }

        } else if (parent == null) {
            parent = new ZeroStream(descriptor.getSizeInSectors() * Sizes.Sector);
        }

        if (monolithicStream != null) {
            return new HostedSparseExtentStream(monolithicStream, Ownership.None, diskOffset, parent, ownsParent);
        }

        // Early-out for monolithic VMDKs
        return switch (descriptor.getType()) {
            case Flat, Vmfs ->
                    MappedStream.fromStream(fileLocator.open(descriptor.getFileName(), FileMode.Open, access, share),
                            Ownership.Dispose);
            case Zero -> new ZeroStream(descriptor.getSizeInSectors() * Sizes.Sector);
            case Sparse ->
                    new HostedSparseExtentStream(fileLocator.open(descriptor.getFileName(), FileMode.Open, access, share),
                            Ownership.Dispose,
                            diskOffset,
                            parent,
                            ownsParent);
            case VmfsSparse ->
                    new ServerSparseExtentStream(fileLocator.open(descriptor.getFileName(), FileMode.Open, access, share),
                            Ownership.Dispose,
                            diskOffset,
                            parent,
                            ownsParent);
            default -> throw new UnsupportedOperationException();
        };
    }
}
