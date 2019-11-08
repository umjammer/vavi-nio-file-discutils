//
// Copyright (c) 2014, Quamotion
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


public class AttributeKey extends BTreeKey<AttributeKey> implements XComparable<AttributeKey> {
    private short _keyLength;

    @SuppressWarnings("unused")
    private short _pad;

    @SuppressWarnings("unused")
    private int _startBlock;

    public AttributeKey() {
    }

    public AttributeKey(CatalogNodeId nodeId, String name) {
        _fileId = nodeId;
        _name = name;
    }

    private CatalogNodeId _fileId;

    public CatalogNodeId getFileId() {
        return _fileId;
    }

    public void setFileId(CatalogNodeId value) {
        _fileId = value;
    }

    private String _name;

    public String getName() {
        return _name;
    }

    public void setName(String value) {
        _name = value;
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        _keyLength = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        _pad = EndianUtilities.toUInt16BigEndian(buffer, offset + 2);
        _fileId = new CatalogNodeId(EndianUtilities.toUInt32BigEndian(buffer, offset + 4));
        _startBlock = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        _name = HfsPlusUtilities.readUniStr255(buffer, offset + 12);
        return _keyLength + 2;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public int compareTo(BTreeKey<?> other) {
        return compareTo(other instanceof AttributeKey ? (AttributeKey) other : (AttributeKey) null);
    }

    public int compareTo(AttributeKey other) {
        if (other == null) {
            throw new NullPointerException("other");
        }

        if (getFileId() != other.getFileId()) {
            return getFileId().getId() < other.getFileId().getId() ? -1 : 1;
        }

        return HfsPlusUtilities.fastUnicodeCompare(getName(), other.getName());
    }

    public String toString() {
        return _name + " (" + _fileId + ")";
    }
}
