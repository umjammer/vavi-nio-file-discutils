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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import DiscUtils.Streams.Util.EndianUtilities;


/**
 * Class providing information about a VHDX region table.
 */
public final class RegionTableInfo implements Iterable<RegionInfo> {
    private final RegionTable _table;

    public RegionTableInfo(RegionTable table) {
        _table = table;
    }

    /**
     * Gets the checksum of the region table.
     */
    public int getChecksum() {
        return _table.Checksum;
    }

    private List<RegionInfo> getEntries() {
        return _table.Regions.values().stream().map(RegionInfo::new).collect(Collectors.toList());
    }

    /**
     * Gets the signature of the region table.
     */
    public String getSignature() {
        byte[] buffer = new byte[4];
        EndianUtilities.writeBytesLittleEndian(_table.Signature, buffer, 0);
        return EndianUtilities.bytesToString(buffer, 0, 4);
    }

    /**
     * Gets the number of metadata items present.
     */
    public int getCount() {
        return _table.EntryCount;
    }

    /**
     * Gets a value indicating whether this table is read-only (always true).
     */
    public boolean getIsReadOnly() {
        return true;
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param item The item to add.
     */
    public void add(RegionInfo item) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Determines if the specified region is present already.
     *
     * The comparison is based on the region identity.
     *
     * @param item The item to look for.
     * @return {@code true} if present, else {@code false}.
     */
    public boolean contains(RegionInfo item) {
        for (Map.Entry<UUID, RegionEntry> entry : _table.Regions.entrySet()) {
            if (entry.getKey().equals(item.getGuid())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies this region table to an array.
     *
     * @param array The destination array.
     * @param arrayIndex The index of the first item to populate in the array.
     */
    public void copyTo(RegionInfo[] array, int arrayIndex) {
        int offset = 0;
        for (Map.Entry<UUID, RegionEntry> entry : _table.Regions.entrySet()) {
            array[arrayIndex + offset] = new RegionInfo(entry.getValue());
            ++offset;
        }
    }

    /**
     * Removes an item from the table.
     *
     * Always throws {@link UnsupportedOperationException }as the table is read-only.
     *
     * @param item The item to remove.
     * @return {@code true} if the item was removed, else {@code false}.
     */
    public boolean remove(RegionInfo item) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets an enumerator for the regions.
     *
     * @return A new enumerator.
     */
    public Iterator<RegionInfo> iterator() {
        return getEntries().iterator();
    }
}
