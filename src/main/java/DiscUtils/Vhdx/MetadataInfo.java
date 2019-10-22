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

import java.util.UUID;


/**
 * Class representing an individual metadata item.
 */
public final class MetadataInfo {
    private final MetadataEntry _entry;

    public MetadataInfo(MetadataEntry entry) {
        _entry = entry;
    }

    /**
     * Gets a value indicating whether parsing this metadata is needed to open
     * the VHDX file.
     */
    public boolean getIsRequired() {
        return _entry.Flags.contains(MetadataEntryFlags.IsRequired);
    }

    /**
     * Gets a value indicating whether this is system or user metadata.
     */
    public boolean isUser() {
        return _entry.Flags.contains(MetadataEntryFlags.IsUser);
    }

    /**
     * Gets a value indicating whether this is virtual disk metadata, or VHDX
     * file metadata.
     */
    public boolean getIsVirtualDisk() {
        return _entry.Flags.contains(MetadataEntryFlags.IsVirtualDisk);
    }

    /**
     * Gets the unique identifier for the metadata.
     */
    public UUID getItemId() {
        return _entry.ItemId;
    }

    /**
     * Gets the length of the metadata.
     */
    public long getLength() {
        return _entry.Length;
    }

    /**
     * Gets the offset within the metadata region of the metadata.
     */
    public long getOffset() {
        return _entry.Offset;
    }

    /**
     * Gets the descriptive name for well-known metadata.
     */
    public String getWellKnownName() {
        if (_entry.ItemId.equals(MetadataTable.FileParametersGuid)) {
            return "File Parameters";
        }

        if (_entry.ItemId.equals(MetadataTable.LogicalSectorSizeGuid)) {
            return "Logical Sector Size";
        }

        if (_entry.ItemId.equals(MetadataTable.Page83DataGuid)) {
            return "SCSI Page 83 Data";
        }

        if (_entry.ItemId.equals(MetadataTable.ParentLocatorGuid)) {
            return "Parent Locator";
        }

        if (_entry.ItemId.equals(MetadataTable.PhysicalSectorSizeGuid)) {
            return "Physical Sector Size";
        }

        if (_entry.ItemId.equals(MetadataTable.VirtualDiskSizeGuid)) {
            return "Virtual Disk Size";
        }

        return null;
    }
}
