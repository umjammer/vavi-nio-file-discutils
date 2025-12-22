//
// Copyright (c) 2008-2012, Kenneth Bell
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

import java.util.UUID;


public final class MetadataEntryKey {

    private final UUID itemId;

    public MetadataEntryKey(UUID itemId, boolean isUser) {
        this.itemId = itemId;
        this.isUser = isUser;
    }

    private final boolean isUser;

    public boolean isUser() {
        return isUser;
    }

    public UUID getItemId() {
        return itemId;
    }

    public boolean equals(MetadataEntryKey other) {
        if (other == null) {
            return false;
        }

        return itemId.equals(other.itemId) && isUser == other.isUser;
    }

    public static MetadataEntryKey fromEntry(MetadataEntry entry) {
        return new MetadataEntryKey(entry.itemId, entry.flags.contains(MetadataEntryFlags.IsUser));
    }

    public boolean equals(Object other) {
        MetadataEntryKey otherKey = other instanceof MetadataEntryKey ? (MetadataEntryKey) other : null;
        if (otherKey != null) {
            return equals(otherKey);
        }

        return false;
    }

    public int hashCode() {
        return itemId.hashCode() ^ (isUser ? 0x3C13A5 : 0);
    }

    public String toString() {
        return itemId + (isUser ? " - User" : " - System");
    }
}
