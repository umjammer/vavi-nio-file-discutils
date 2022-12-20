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

package discUtils.vhdx;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import vavi.util.ByteUtil;


/**
 * Detailed information about a VHDX file.
 */
public final class DiskImageFileInfo {

    private final LogSequence activeLogSequence;

    private final FileHeader fileHeader;

    private final Metadata metadata;

    private final RegionTable regions;

    private final VhdxHeader vhdxHeader1;

    private final VhdxHeader vhdxHeader2;

    public DiskImageFileInfo(FileHeader fileHeader,
            VhdxHeader vhdxHeader1,
            VhdxHeader vhdxHeader2,
            RegionTable regions,
            Metadata metadata,
            LogSequence activeLogSequence) {
        this.fileHeader = fileHeader;
        this.vhdxHeader1 = vhdxHeader1;
        this.vhdxHeader2 = vhdxHeader2;
        this.regions = regions;
        this.metadata = metadata;
        this.activeLogSequence = activeLogSequence;
    }

    /**
     * Gets the active header for the VHDX file.
     */
    public HeaderInfo getActiveHeader() {
        if (vhdxHeader1 == null) {
            if (vhdxHeader2 == null) {
                return null;
            }

            return new HeaderInfo(vhdxHeader2);
        }

        if (vhdxHeader2 == null) {
            return new HeaderInfo(vhdxHeader1);
        }

        return new HeaderInfo(vhdxHeader1.sequenceNumber > vhdxHeader2.sequenceNumber ? vhdxHeader1 : vhdxHeader2);
    }

    /**
     * Gets the active log sequence for this VHDX file.
     */
    public List<LogEntryInfo> getActiveLogSequence() {
        if (activeLogSequence != null) {
            return activeLogSequence.stream().map(LogEntryInfo::new).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Gets the block size of the VHDX file.
     */
    public long getBlockSize() {
        return metadata.getFileParameters().blockSize;
    }

    /**
     * Gets the VHDX 'parser' that created the VHDX file.
     */
    public String getCreator() {
        return fileHeader.creator;
    }

    /**
     * Gets the logical size of the disk represented by the VHDX file.
     */
    public long getDiskSize() {
        return metadata.getDiskSize();
    }

    /**
     * Gets the first header (by file location) of the VHDX file.
     */
    public HeaderInfo getFirstHeader() {
        return new HeaderInfo(vhdxHeader1);
    }

    /**
     * Gets a value indicating whether the VHDX file has a parent file (i.e. is
     * a differencing file).
     */
    public boolean hasParent()  {
        return metadata.getFileParameters().flags.contains(FileParametersFlags.HasParent);
    }

    /**
     * Gets a value indicating whether blocks should be left allocated within
     * the file.
     */
    public boolean leaveBlocksAllocated() {
        return metadata.getFileParameters().flags.contains(FileParametersFlags.LeaveBlocksAllocated);
    }

    /**
     * Gets the logical sector size of the disk represented by the VHDX file.
     */
    public long getLogicalSectorSize() {
        return metadata.getLogicalSectorSize();
    }

    /**
     * Gets the metadata table of the VHDX file.
     */
    public MetadataTableInfo getMetadataTable() {
        return new MetadataTableInfo(metadata.getTable());
    }

    /**
     * Gets the set of parent locators, for differencing files.
     */
    public Map<String, String> getParentLocatorEntries() {
        return metadata.getParentLocator() != null ? metadata.getParentLocator().getEntries() : Collections.emptyMap();
    }

    /**
     * Gets the parent locator type, for differencing files.
     */
    public UUID getParentLocatorType() {
        return metadata.getParentLocator() != null ? metadata.getParentLocator().locatorType : new UUID(0L, 0L);
    }

    /**
     * Gets the physical sector size of disk represented by the VHDX file.
     */
    public long getPhysicalSectorSize() {
        return metadata.getPhysicalSectorSize();
    }

    /**
     * Gets the region table of the VHDX file.
     */
    public RegionTableInfo getRegionTable() {
        return new RegionTableInfo(regions);
    }

    /**
     * Gets the second header (by file location) of the VHDX file.
     */
    public HeaderInfo getSecondHeader() {
        return new HeaderInfo(vhdxHeader2);
    }

    /**
     * Gets the file signature.
     */
    public String getSignature() {
        byte[] buffer = new byte[8];
        ByteUtil.writeLeLong(fileHeader.signature, buffer, 0);
        return new String(buffer, 0, 8, StandardCharsets.US_ASCII);
    }
}
