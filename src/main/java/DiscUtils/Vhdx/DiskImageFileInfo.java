//
// Copyright (c) 2008-2013, Kenneth Bell
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

package DiscUtils.Vhdx;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import DiscUtils.Streams.Util.EndianUtilities;


/**
 * Detailed information about a VHDX file.
 */
public final class DiskImageFileInfo {
    private final LogSequence _activeLogSequence;

    private final FileHeader _fileHeader;

    private final Metadata _metadata;

    private final RegionTable _regions;

    private final VhdxHeader _vhdxHeader1;

    private final VhdxHeader _vhdxHeader2;

    public DiskImageFileInfo(FileHeader fileHeader,
            VhdxHeader vhdxHeader1,
            VhdxHeader vhdxHeader2,
            RegionTable regions,
            Metadata metadata,
            LogSequence activeLogSequence) {
        _fileHeader = fileHeader;
        _vhdxHeader1 = vhdxHeader1;
        _vhdxHeader2 = vhdxHeader2;
        _regions = regions;
        _metadata = metadata;
        _activeLogSequence = activeLogSequence;
    }

    /**
     * Gets the active header for the VHDX file.
     */
    public HeaderInfo getActiveHeader() {
        if (_vhdxHeader1 == null) {
            if (_vhdxHeader2 == null) {
                return null;
            }

            return new HeaderInfo(_vhdxHeader2);
        }

        if (_vhdxHeader2 == null) {
            return new HeaderInfo(_vhdxHeader1);
        }

        return new HeaderInfo(_vhdxHeader1.SequenceNumber > _vhdxHeader2.SequenceNumber ? _vhdxHeader1 : _vhdxHeader2);
    }

    /**
     * Gets the active log sequence for this VHDX file.
     */
    public List<LogEntryInfo> getActiveLogSequence() {
        if (_activeLogSequence != null) {
            return _activeLogSequence.stream().map(entry -> new LogEntryInfo(entry)).collect(Collectors.toList());
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Gets the block size of the VHDX file.
     */
    public long getBlockSize() {
        return _metadata.getFileParameters().BlockSize;
    }

    /**
     * Gets the VHDX 'parser' that created the VHDX file.
     */
    public String getCreator() {
        return _fileHeader.Creator;
    }

    /**
     * Gets the logical size of the disk represented by the VHDX file.
     */
    public long getDiskSize() {
        return _metadata.getDiskSize();
    }

    /**
     * Gets the first header (by file location) of the VHDX file.
     */
    public HeaderInfo getFirstHeader() {
        return new HeaderInfo(_vhdxHeader1);
    }

    /**
     * Gets a value indicating whether the VHDX file has a parent file (i.e. is
     * a differencing file).
     */
    public boolean hasParent()  {
        return _metadata.getFileParameters().Flags.contains(FileParametersFlags.HasParent);
    }

    /**
     * Gets a value indicating whether blocks should be left allocated within
     * the file.
     */
    public boolean leaveBlocksAllocated() {
        return _metadata.getFileParameters().Flags.contains(FileParametersFlags.LeaveBlocksAllocated);
    }

    /**
     * Gets the logical sector size of the disk represented by the VHDX file.
     */
    public long getLogicalSectorSize() {
        return _metadata.getLogicalSectorSize();
    }

    /**
     * Gets the metadata table of the VHDX file.
     */
    public MetadataTableInfo getMetadataTable() {
        return new MetadataTableInfo(_metadata.getTable());
    }

    /**
     * Gets the set of parent locators, for differencing files.
     */
    public Map<String, String> getParentLocatorEntries() {
        return _metadata.getParentLocator() != null ? _metadata.getParentLocator().getEntries() : Collections.EMPTY_MAP;
    }

    /**
     * Gets the parent locator type, for differencing files.
     */
    public UUID getParentLocatorType() {
        return _metadata.getParentLocator() != null ? _metadata.getParentLocator().LocatorType : new UUID(0L, 0L);
    }

    /**
     * Gets the physical sector size of disk represented by the VHDX file.
     */
    public long getPhysicalSectorSize() {
        return _metadata.getPhysicalSectorSize();
    }

    /**
     * Gets the region table of the VHDX file.
     */
    public RegionTableInfo getRegionTable() {
        return new RegionTableInfo(_regions);
    }

    /**
     * Gets the second header (by file location) of the VHDX file.
     */
    public HeaderInfo getSecondHeader() {
        return new HeaderInfo(_vhdxHeader2);
    }

    /**
     * Gets the file signature.
     */
    public String getSignature() {
        byte[] buffer = new byte[8];
        EndianUtilities.writeBytesLittleEndian(_fileHeader.Signature, buffer, 0);
        return EndianUtilities.bytesToString(buffer, 0, 8);
    }

}
