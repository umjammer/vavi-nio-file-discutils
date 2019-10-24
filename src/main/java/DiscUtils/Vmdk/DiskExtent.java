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

package DiscUtils.Vmdk;

import java.io.IOException;

import DiscUtils.Core.FileLocator;
import DiscUtils.Core.VirtualDiskExtent;
import DiscUtils.Streams.MappedStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.ZeroStream;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


public final class DiskExtent extends VirtualDiskExtent {
    private FileAccess _access;

    private final ExtentDescriptor _descriptor;

    private final long _diskOffset;

    private FileLocator _fileLocator;

    private Stream _monolithicStream;

    public DiskExtent(ExtentDescriptor descriptor, long diskOffset, FileLocator fileLocator, FileAccess access) {
        _descriptor = descriptor;
        _diskOffset = diskOffset;
        _fileLocator = fileLocator;
        _access = access;
    }

    public DiskExtent(ExtentDescriptor descriptor, long diskOffset, Stream monolithicStream) {
        _descriptor = descriptor;
        _diskOffset = diskOffset;
        _monolithicStream = monolithicStream;
    }

    public long getCapacity() {
        return _descriptor.getSizeInSectors() * Sizes.Sector;
    }

    public boolean isSparse() {
        return _descriptor.getType() == ExtentType.Sparse || _descriptor.getType() == ExtentType.VmfsSparse ||
               _descriptor.getType() == ExtentType.Zero;
    }

    public long getStoredSize() {
        if (_monolithicStream != null) {
            return _monolithicStream.getLength();
        }

        try (Stream s = _fileLocator.open(_descriptor.getFileName(), FileMode.Open, FileAccess.Read, FileShare.Read)) {
            return s.getLength();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public MappedStream openContent(SparseStream parent, Ownership ownsParent) throws IOException {
        FileAccess access = FileAccess.Read;
        FileShare share = FileShare.Read;
        if (_descriptor.getAccess() == ExtentAccess.ReadWrite && _access != FileAccess.Read) {
            access = FileAccess.ReadWrite;
            share = FileShare.None;
        }

        if (_descriptor.getType() != ExtentType.Sparse && _descriptor.getType() != ExtentType.VmfsSparse &&
            _descriptor.getType() != ExtentType.Zero) {
            if (ownsParent == Ownership.Dispose && parent != null) {
                try {
                    parent.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }

        } else if (parent == null) {
            parent = new ZeroStream(_descriptor.getSizeInSectors() * Sizes.Sector);
        }

        if (_monolithicStream != null) {
            return new HostedSparseExtentStream(_monolithicStream, Ownership.None, _diskOffset, parent, ownsParent);
        }

        // Early-out for monolithic VMDKs
        switch (_descriptor.getType()) {
        case Flat:
        case Vmfs:
            return MappedStream.fromStream(_fileLocator.open(_descriptor.getFileName(), FileMode.Open, access, share),
                                           Ownership.Dispose);
        case Zero:
            return new ZeroStream(_descriptor.getSizeInSectors() * Sizes.Sector);
        case Sparse:
            return new HostedSparseExtentStream(_fileLocator.open(_descriptor.getFileName(), FileMode.Open, access, share),
                                                Ownership.Dispose,
                                                _diskOffset,
                                                parent,
                                                ownsParent);
        case VmfsSparse:
            return new ServerSparseExtentStream(_fileLocator.open(_descriptor.getFileName(), FileMode.Open, access, share),
                                                Ownership.Dispose,
                                                _diskOffset,
                                                parent,
                                                ownsParent);
        default:
            throw new UnsupportedOperationException();

        }
    }
}
