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
 * Class representing a region in a VHDX file.
 */
public final class RegionInfo {
    private final RegionEntry _entry;

    public RegionInfo(RegionEntry entry) {
        _entry = entry;
    }

    /**
     * Gets the file offset of this region within the VHDX file.
     */
    public long getFileOffset() {
        return _entry.FileOffset;
    }

    /**
     * Gets the unique identifier for this region.
     */
    public UUID getGuid() {
        return _entry.Guid;
    }

    /**
     * Gets a value indicating whether this region is required.
     *
     * To load a VHDX file, a parser must be able to interpret all regions
     * marked as required.
     */
    public boolean getIsRequired() {
        return _entry.Flags == RegionFlags.Required;
    }

    /**
     * Gets the length of this region within the VHDX file.
     */
    public long getLength() {
        return _entry.Length;
    }

    /**
     * Gets the well-known name (if any) of the region.
     *
     * VHDX 1.0 specification defines the "BAT" and "Metadata Region", unknown
     * regions
     * will return as
     * {@code null}
     * .
     */
    public String getWellKnownName() {
        if (_entry.Guid == RegionEntry.BatGuid) {
            return "BAT";
        }

        if (_entry.Guid == RegionEntry.MetadataRegionGuid) {
            return "Metadata Region";
        }

        return null;
    }

}