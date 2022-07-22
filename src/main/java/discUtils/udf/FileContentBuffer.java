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

package discUtils.udf;

import java.util.ArrayList;
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.EndianUtilities;


public class FileContentBuffer implements IBuffer {

    private final int blockSize;

    private final UdfContext context;

    private List<CookedExtent> extents;

    private final FileEntry fileEntry;

    private final Partition partition;

    public FileContentBuffer(UdfContext context, Partition partition, FileEntry fileEntry, int blockSize) {
        this.context = context;
        this.partition = partition;
        this.fileEntry = fileEntry;
        this.blockSize = blockSize;
        loadExtents();
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public long getCapacity() {
        return fileEntry.informationLength;
    }

    public List<StreamExtent> getExtents() {
        throw new UnsupportedOperationException();
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        if (fileEntry.informationControlBlock.allocationType == AllocationType.Embedded) {
            byte[] srcBuffer = fileEntry.allocationDescriptors;
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
        extents = new ArrayList<>();
        byte[] activeBuffer = fileEntry.allocationDescriptors;

        AllocationType allocType = fileEntry.informationControlBlock.allocationType;
        if (allocType == AllocationType.ShortDescriptors) {
            long filePos = 0;

            int i = 0;
            while (i < activeBuffer.length) {
                ShortAllocationDescriptor sad =
                        EndianUtilities.toStruct(ShortAllocationDescriptor.class, activeBuffer, i);
                if (sad.extentLength == 0) {
                    break;
                }

                if (sad.flags != ShortAllocationFlags.RecordedAndAllocated) {
                    throw new UnsupportedOperationException("Extents that are not 'recorded and allocated' not implemented");
                }

                CookedExtent newExtent = new CookedExtent();
                newExtent.fileContentOffset = filePos;
                newExtent.partition = Integer.MAX_VALUE;
                newExtent.startPos = sad.extentLocation * (long) blockSize;
                newExtent.length = sad.extentLength;
                extents.add(newExtent);

                filePos += sad.extentLength;
                i += sad.size();
            }
        } else if (allocType == AllocationType.Embedded) {
            // do nothing
        } else if (allocType == AllocationType.LongDescriptors) {
            long filePos = 0;

            int i = 0;
            while (i < activeBuffer.length) {
                LongAllocationDescriptor lad = EndianUtilities
                        .toStruct(LongAllocationDescriptor.class, activeBuffer, i);
                if (lad.extentLength == 0) {
                    break;
                }

                CookedExtent newExtent = new CookedExtent();
                newExtent.fileContentOffset = filePos;
                newExtent.partition = lad.extentLocation.getPartition();
                newExtent.startPos = lad.extentLocation.logicalBlock * (long) blockSize;
                newExtent.length = lad.extentLength;
                extents.add(newExtent);

                filePos += lad.extentLength;
                i += lad.size();
            }
        } else {
            throw new UnsupportedOperationException("Allocation Type: " + fileEntry.informationControlBlock.allocationType);
        }
    }

    private int readFromExtents(long pos, byte[] buffer, int offset, int count) {
        int totalToRead = (int) Math.min(getCapacity() - pos, count);
        int totalRead = 0;
        while (totalRead < totalToRead) {
            CookedExtent extent = findExtent(pos + totalRead);
            long extentOffset = pos + totalRead - extent.fileContentOffset;
            int toRead = (int) Math.min(totalToRead - totalRead, extent.length - extentOffset);
            Partition part;
            if (extent.partition != Integer.MAX_VALUE) {
                part = context.logicalPartitions.get(extent.partition);
            } else {
                part = partition;
            }
            int numRead = part.getContent().read(extent.startPos + extentOffset, buffer, offset + totalRead, toRead);
            if (numRead == 0) {
                return totalRead;
            }

            totalRead += numRead;
        }
        return totalRead;
    }

    private CookedExtent findExtent(long pos) {
        for (CookedExtent extent : extents) {
            if (extent.fileContentOffset + extent.length > pos) {
                return extent;
            }
        }
        return null;
    }

    private static class CookedExtent {
        public long fileContentOffset;

        public long length;

        public int partition;

        public long startPos;
    }
}
