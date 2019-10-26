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

package DiscUtils.HfsPlus;

import DiscUtils.Streams.Util.EndianUtilities;


public final class CatalogKey extends BTreeKey<CatalogKey> implements YComparable<CatalogKey> {
    private short _keyLength;

    public CatalogKey() {
    }

    public CatalogKey(CatalogNodeId nodeId, String name) {
        setNodeId(nodeId);
        setName(name);
    }

    private String __Name;

    public String getName() {
        return __Name;
    }

    public void setName(String value) {
        __Name = value;
    }

    private CatalogNodeId __NodeId;

    public CatalogNodeId getNodeId() {
        return __NodeId;
    }

    public void setNodeId(CatalogNodeId value) {
        __NodeId = value;
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int compareTo(CatalogKey other) {
        if (other == null) {
            throw new NullPointerException("other");
        }

        if (getNodeId() != other.getNodeId()) {
            return getNodeId().getId() < other.getNodeId().getId() ? -1 : 1;
        }

        return HfsPlusUtilities.fastUnicodeCompare(getName(), other.getName());
    }

    public int readFrom(byte[] buffer, int offset) {
        _keyLength = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        setNodeId(new CatalogNodeId(EndianUtilities.toUInt32BigEndian(buffer, offset + 2)));
        setName(HfsPlusUtilities.readUniStr255(buffer, offset + 6));
        return _keyLength + 2;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public int compareTo(BTreeKey<?> other) {
        return compareTo(other instanceof CatalogKey ? (CatalogKey) other : (CatalogKey) null);
    }

    public String toString() {
        return getName() + " (" + getNodeId() + ")";
    }
}
