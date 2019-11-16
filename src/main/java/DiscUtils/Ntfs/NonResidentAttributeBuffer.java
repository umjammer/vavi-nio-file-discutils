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

package DiscUtils.Ntfs;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import DiscUtils.Streams.Util.MathUtilities;
import dotnet4j.io.IOException;


public class NonResidentAttributeBuffer extends NonResidentDataBuffer {
    private final NtfsAttribute _attribute;

    private final File _file;

    public NonResidentAttributeBuffer(File file, NtfsAttribute attribute) {
        super(file.getContext(), cookRuns(attribute), file.getIndexInMft() == MasterFileTable.MftIndex);

        _file = file;
        _attribute = attribute;

        if (attribute.getFlags().contains(AttributeFlags.Sparse)) {
            _activeStream = new SparseClusterStream(_attribute, _rawStream);
        } else if (attribute.getFlags().contains(AttributeFlags.Compressed)) {
            _activeStream = new CompressedClusterStream(_context, _attribute, _rawStream);
        } else if (attribute.getFlags().isEmpty()) {
            _activeStream = _rawStream;
        } else {
            throw new UnsupportedOperationException("Unhandled attribute type '" + attribute.getFlags() + "'");
        }
    }

    public boolean canWrite() {
        return _context.getRawStream().canWrite() && _file != null;
    }

    public long getCapacity() {
        return getPrimaryAttributeRecord().getDataLength();
    }

    private NonResidentAttributeRecord getPrimaryAttributeRecord() {
        return _attribute.getPrimaryRecord() instanceof NonResidentAttributeRecord
                                                                                   ? (NonResidentAttributeRecord) _attribute
                                                                                           .getPrimaryRecord()
                                                                                   : (NonResidentAttributeRecord) null;
    }

    public void alignVirtualClusterCount() {
        _file.markMftRecordDirty();
        _activeStream.expandToClusters(MathUtilities.ceil(_attribute.getLength(), _bytesPerCluster),
                                       (NonResidentAttributeRecord) _attribute.getLastExtent(),
                                       false);
    }

    public void setCapacity(long value) {
        if (!canWrite()) {
            throw new IOException("Attempt to change length of file not opened for write");
        }

        if (value == getCapacity()) {
            return;
        }

        _file.markMftRecordDirty();

        long newClusterCount = MathUtilities.ceil(value, _bytesPerCluster);

        if (value < getCapacity()) {
            truncate(value);
        } else {
            _activeStream.expandToClusters(newClusterCount, (NonResidentAttributeRecord) _attribute.getLastExtent(), true);
            getPrimaryAttributeRecord().setAllocatedLength(_cookedRuns.getNextVirtualCluster() * _bytesPerCluster);
        }

        getPrimaryAttributeRecord().setDataLength(value);

        if (getPrimaryAttributeRecord().getInitializedDataLength() > value) {
            getPrimaryAttributeRecord().setInitializedDataLength(value);
        }

        _cookedRuns.collapseRuns();
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        if (!canWrite()) {
            throw new IOException("Attempt to write to file not opened for write");
        }

        if (count == 0) {
            return;
        }

        if (pos + count > getCapacity()) {
            setCapacity(pos + count);
        }

        // Write zeros from end of current initialized data to the start of the
        // new write
        if (pos > getPrimaryAttributeRecord().getInitializedDataLength()) {
            initializeData(pos);
        }

        int allocatedClusters = 0;

        long focusPos = pos;
        while (focusPos < pos + count) {
            long vcn = focusPos / _bytesPerCluster;
            long remaining = pos + count - focusPos;
            long clusterOffset = focusPos - vcn * _bytesPerCluster;

            if (vcn * _bytesPerCluster != focusPos || remaining < _bytesPerCluster) {
                // Unaligned or short write
                int toWrite = (int) Math.min(remaining, _bytesPerCluster - clusterOffset);

                _activeStream.readClusters(vcn, 1, _ioBuffer, 0);
                System.arraycopy(buffer, (int) (offset + (focusPos - pos)), _ioBuffer, (int) clusterOffset, toWrite);
                allocatedClusters += _activeStream.writeClusters(vcn, 1, _ioBuffer, 0);

                focusPos += toWrite;
            } else {
                // Aligned, full cluster writes...
                int fullClusters = (int) (remaining / _bytesPerCluster);
                allocatedClusters += _activeStream.writeClusters(vcn, fullClusters, buffer, (int) (offset + (focusPos - pos)));

                focusPos += fullClusters * _bytesPerCluster;
            }
        }

        if (pos + count > getPrimaryAttributeRecord().getInitializedDataLength()) {
            _file.markMftRecordDirty();

            getPrimaryAttributeRecord().setInitializedDataLength(pos + count);
        }

        if (pos + count > getPrimaryAttributeRecord().getDataLength()) {
            _file.markMftRecordDirty();

            getPrimaryAttributeRecord().setDataLength(pos + count);
        }

        if (!Collections.disjoint(_attribute.getFlags(), EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            getPrimaryAttributeRecord().setCompressedDataSize(getPrimaryAttributeRecord().getCompressedDataSize() +
                                                              allocatedClusters * _bytesPerCluster);
        }

        _cookedRuns.collapseRuns();
    }

    public void clear(long pos, int count) {
        if (!canWrite()) {
            throw new IOException("Attempt to erase bytes from file not opened for write");
        }

        if (count == 0) {
            return;
        }

        if (pos + count > getCapacity()) {
            setCapacity(pos + count);
        }

        _file.markMftRecordDirty();

        // Write zeros from end of current initialized data to the start of the
        // new write
        if (pos > getPrimaryAttributeRecord().getInitializedDataLength()) {
            initializeData(pos);
        }

        int releasedClusters = 0;

        long focusPos = pos;
        while (focusPos < pos + count) {
            long vcn = focusPos / _bytesPerCluster;
            long remaining = pos + count - focusPos;
            long clusterOffset = focusPos - vcn * _bytesPerCluster;

            if (vcn * _bytesPerCluster != focusPos || remaining < _bytesPerCluster) {
                // Unaligned or short write
                int toClear = (int) Math.min(remaining, _bytesPerCluster - clusterOffset);

                if (_activeStream.isClusterStored(vcn)) {
                    _activeStream.readClusters(vcn, 1, _ioBuffer, 0);
                    Arrays.fill(_ioBuffer, (int) clusterOffset, (int) clusterOffset + toClear, (byte) 0);
                    releasedClusters -= _activeStream.writeClusters(vcn, 1, _ioBuffer, 0);
                }

                focusPos += toClear;
            } else {
                // Aligned, full cluster clears...
                int fullClusters = (int) (remaining / _bytesPerCluster);
                releasedClusters += _activeStream.clearClusters(vcn, fullClusters);

                focusPos += fullClusters * _bytesPerCluster;
            }
        }

        if (pos + count > getPrimaryAttributeRecord().getInitializedDataLength()) {
            getPrimaryAttributeRecord().setInitializedDataLength(pos + count);
        }

        if (pos + count > getPrimaryAttributeRecord().getDataLength()) {
            getPrimaryAttributeRecord().setDataLength(pos + count);
        }

        if (!Collections.disjoint(_attribute.getFlags(), EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            getPrimaryAttributeRecord().setCompressedDataSize(getPrimaryAttributeRecord().getCompressedDataSize() -
                                                              releasedClusters * _bytesPerCluster);
        }

        _cookedRuns.collapseRuns();
    }

//public static boolean debug;

    private static CookedDataRuns cookRuns(NtfsAttribute attribute) {
        CookedDataRuns result = new CookedDataRuns();

//if (debug) Debug.println("3: " + attribute.getRecords().size());
        for (Object _record : attribute.getRecords()) {
            NonResidentAttributeRecord record = (NonResidentAttributeRecord) _record;
            if (record.getStartVcn() != result.getNextVirtualCluster()) {
                throw new IOException("Invalid NTFS attribute - non-contiguous data runs");
            }

            result.append(record.getDataRuns(), record);
        }

        return result;
    }

    private void initializeData(long pos) {
        long initDataLen = getPrimaryAttributeRecord().getInitializedDataLength();
        _file.markMftRecordDirty();

        int clustersAllocated = 0;

        while (initDataLen < pos) {
            long vcn = initDataLen / _bytesPerCluster;
            if (initDataLen % _bytesPerCluster != 0 || pos - initDataLen < _bytesPerCluster) {
                int clusterOffset = (int) (initDataLen - vcn * _bytesPerCluster);
                int toClear = (int) Math.min(_bytesPerCluster - clusterOffset, pos - initDataLen);

                if (_activeStream.isClusterStored(vcn)) {
                    _activeStream.readClusters(vcn, 1, _ioBuffer, 0);
                    Arrays.fill(_ioBuffer, clusterOffset, clusterOffset + toClear, (byte) 0);
                    clustersAllocated += _activeStream.writeClusters(vcn, 1, _ioBuffer, 0);
                }

                initDataLen += toClear;
            } else {
                int numClusters = (int) (pos / _bytesPerCluster - vcn);
                clustersAllocated -= _activeStream.clearClusters(vcn, numClusters);

                initDataLen += numClusters * _bytesPerCluster;
            }
        }

        getPrimaryAttributeRecord().setInitializedDataLength(pos);

        if (!Collections.disjoint(_attribute.getFlags(), EnumSet.of(AttributeFlags.Compressed, AttributeFlags.Sparse))) {
            getPrimaryAttributeRecord().setCompressedDataSize(getPrimaryAttributeRecord().getCompressedDataSize() +
                                                              clustersAllocated * _bytesPerCluster);
        }
    }

    private void truncate(long value) {
        long endVcn = MathUtilities.ceil(value, _bytesPerCluster);

        // Release the clusters
        _activeStream.truncateToClusters(endVcn);

        // First, remove any extents that are now redundant.
        Map<AttributeReference, AttributeRecord> extentCache = new HashMap<>(_attribute.getExtents());
        for (Map.Entry<AttributeReference, AttributeRecord> extent : extentCache.entrySet()) {
            if (extent.getValue().getStartVcn() >= endVcn) {
                NonResidentAttributeRecord record = (NonResidentAttributeRecord) extent.getValue();
                _file.removeAttributeExtent(extent.getKey());
                _attribute.removeExtentCacheSafe(extent.getKey());
            }
        }

        getPrimaryAttributeRecord().setLastVcn(Math.max(0, endVcn - 1));
        getPrimaryAttributeRecord().setAllocatedLength(endVcn * _bytesPerCluster);
        getPrimaryAttributeRecord().setDataLength(value);
        getPrimaryAttributeRecord()
                .setInitializedDataLength(Math.min(getPrimaryAttributeRecord().getInitializedDataLength(), value));

        _file.markMftRecordDirty();
    }
}
