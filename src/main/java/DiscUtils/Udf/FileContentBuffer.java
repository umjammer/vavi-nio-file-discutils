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

package DiscUtils.Udf;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;


public class FileContentBuffer implements IBuffer {
    private final int _blockSize;

    private final UdfContext _context;

    private List<CookedExtent> _extents;

    private final FileEntry _fileEntry;

    private final Partition _partition;

    public FileContentBuffer(UdfContext context, Partition partition, FileEntry fileEntry, int blockSize) {
        _context = context;
        _partition = partition;
        _fileEntry = fileEntry;
        _blockSize = blockSize;
        loadExtents();
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public long getCapacity() {
        return _fileEntry.InformationLength;
    }

    public List<StreamExtent> getExtents() {
        throw new UnsupportedOperationException();
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        if (_fileEntry.InformationControlBlock._AllocationType == AllocationType.Embedded) {
            byte[] srcBuffer = _fileEntry.AllocationDescriptors;
            if (pos > srcBuffer.length) {
                return 0;
            }

            int toCopy = (int) Math.min(srcBuffer.length - pos, count);
            System.arraycopy(srcBuffer, (int) pos, buffer, offset, toCopy);
            return toCopy;
        }

        return readFromExtents(pos, buffer, offset, count);
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public void clear(long pos, int count) {
        throw new UnsupportedOperationException();
    }

    public void flush() {
    }

    public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        throw new UnsupportedOperationException();
    }

    private void loadExtents() {
        _extents = new ArrayList<>();
        byte[] activeBuffer = _fileEntry.AllocationDescriptors;

        AllocationType allocType = _fileEntry.InformationControlBlock._AllocationType;
        if (allocType == AllocationType.ShortDescriptors) {
            long filePos = 0;

            int i = 0;
            while (i < activeBuffer.length) {
                ShortAllocationDescriptor sad = EndianUtilities
                        .<ShortAllocationDescriptor> toStruct(ShortAllocationDescriptor.class, activeBuffer, i);
                if (sad.ExtentLength == 0) {
                    break;
                }

                if (sad.Flags != ShortAllocationFlags.RecordedAndAllocated) {
                    throw new UnsupportedOperationException("Extents that are not 'recorded and allocated' not implemented");
                }

                CookedExtent newExtent = new CookedExtent();
                newExtent.FileContentOffset = filePos;
                newExtent.Partition = 0xffffffff;
                newExtent.StartPos = sad.ExtentLocation * (long)_blockSize;
                newExtent.Length = sad.ExtentLength;
                _extents.add(newExtent);

                filePos += sad.ExtentLength;
                i += sad.sizeOf();
            }
        } else if (allocType == AllocationType.Embedded) {
            // do nothing
        } else if (allocType == AllocationType.LongDescriptors) {
            long filePos = 0;

            int i = 0;
            while (i < activeBuffer.length) {
                LongAllocationDescriptor lad = EndianUtilities
                        .<LongAllocationDescriptor> toStruct(LongAllocationDescriptor.class, activeBuffer, i);
                if (lad.ExtentLength == 0) {
                    break;
                }

                CookedExtent newExtent = new CookedExtent();
                newExtent.FileContentOffset = filePos;
                newExtent.Partition = lad.ExtentLocation.Partition;
                newExtent.StartPos = lad.ExtentLocation.LogicalBlock * (long)_blockSize;
                newExtent.Length = lad.ExtentLength;
                _extents.add(newExtent);

                filePos += lad.ExtentLength;
                i += lad.sizeOf();
            }
        } else {
            throw new UnsupportedOperationException("Allocation Type: " + _fileEntry.InformationControlBlock._AllocationType);
        }
    }

    private int readFromExtents(long pos, byte[] buffer, int offset, int count) {
        int totalToRead = (int) Math.min(getCapacity() - pos, count);
        int totalRead = 0;
        while (totalRead < totalToRead) {
            CookedExtent extent = findExtent(pos + totalRead);
            long extentOffset = pos + totalRead - extent.FileContentOffset;
            int toRead = (int) Math.min(totalToRead - totalRead, extent.Length - extentOffset);
            Partition part;
            if (extent.Partition != 0xffffffff) {
                part = _context.LogicalPartitions.get(extent.Partition);
            } else {
                part = _partition;
            }
            int numRead = part.getContent().read(extent.StartPos + extentOffset, buffer, offset + totalRead, toRead);
            if (numRead == 0) {
                return totalRead;
            }

            totalRead += numRead;
        }
        return totalRead;
    }

    private CookedExtent findExtent(long pos) {
        for (CookedExtent extent : _extents) {
            if (extent.FileContentOffset + extent.Length > pos) {
                return extent;
            }
        }
        return null;
    }

    private static class CookedExtent {
        public long FileContentOffset;

        public long Length;

        public int Partition;

        public long StartPos;
    }
}
