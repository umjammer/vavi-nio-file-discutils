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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import vavi.util.ByteUtil;


/**
 * Class representing the table of file metadata.
 */
public final class MetadataTableInfo implements Iterable<MetadataInfo> {

    private final MetadataTable table;

    public MetadataTableInfo(MetadataTable table) {
        this.table = table;
    }

    private List<MetadataInfo> getEntries() {
        return table.entries.values().stream().map(MetadataInfo::new).collect(Collectors.toList());
    }

    /**
     * Gets the signature of the metadata table.
     */
    public String getSignature() {
        byte[] buffer = new byte[8];
        ByteUtil.writeLeLong(table.signature, buffer, 0);
        return new String(buffer, 0, 8, StandardCharsets.US_ASCII);
    }

    /**
     * Gets the number of metadata items present.
     */
    public int getCount() {
        return table.entryCount;
    }

    /**
     * Gets a value indicating whether this table is read-only (always true).
     */
    public boolean getIsReadOnly() {
        return true;
    }

    /**
     * Always throws UnsupportedOperationException .
     *
     * @param item The item to add.
     */
    public void add(MetadataInfo item) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws UnsupportedOperationException .
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Determines if the specified metadata item is present already.
     *
     * The comparison is based on the metadata item identity, not the value.
     *
     * @param item The item to look for.
     * @return {@code true} if present, else {@code false} .
     */
    public boolean contains(MetadataInfo item) {
        for (Map.Entry<MetadataEntryKey, MetadataEntry> entry : table.entries.entrySet()) {
            if (entry.getKey().getItemId().equals(item.getItemId()) && entry.getKey().isUser() == item.isUser()) {
                return true;
            }

        }
        return false;
    }

    /**
     * Copies this metadata table to an array.
     *
     * @param array The destination array.
     * @param arrayIndex The index of the first item to populate in the array.
     */
    public void copyTo(MetadataInfo[] array, int arrayIndex) {
        int offset = 0;
        for (Map.Entry<MetadataEntryKey, MetadataEntry> entry : table.entries.entrySet()) {
            array[arrayIndex + offset] = new MetadataInfo(entry.getValue());
            ++offset;
        }
    }

    /**
     * Removes an item from the table.
     *
     * Always throws UnsupportedOperationException as the table is read-only.
     *
     * @param item The item to remove.
     * @return {@code true} if the item was removed, else {@code false} .
     */
    public boolean remove(MetadataInfo item) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets an enumerator for the metadata items.
     *
     * @return A new enumerator.
     */
    public Iterator<MetadataInfo> iterator() {
        return getEntries().iterator();
    }
}
