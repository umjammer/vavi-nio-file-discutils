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

package discUtils.hfsPlus;

import vavi.util.ByteUtil;


public class AttributeKey extends BTreeKey<AttributeKey> implements XComparable<AttributeKey> {

    private short keyLength;

    @SuppressWarnings("unused")
    private short pad;

    @SuppressWarnings("unused")
    private int startBlock;

    public AttributeKey() {
    }

    public AttributeKey(CatalogNodeId nodeId, String name) {
        fileId = nodeId;
        this.name = name;
    }

    private CatalogNodeId fileId;

    public CatalogNodeId getFileId() {
        return fileId;
    }

    public void setFileId(CatalogNodeId value) {
        fileId = value;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        keyLength = ByteUtil.readBeShort(buffer, offset + 0);
        pad = ByteUtil.readBeShort(buffer, offset + 2);
        fileId = new CatalogNodeId(ByteUtil.readBeInt(buffer, offset + 4));
        startBlock = ByteUtil.readBeInt(buffer, offset + 8);
        name = HfsPlusUtilities.readUniStr255(buffer, offset + 12);

        return keyLength + 2;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public int compareTo(BTreeKey<?> other) {
        return compareTo(other instanceof AttributeKey ? (AttributeKey) other : null);
    }

    public int compareTo(AttributeKey other) {
        if (other == null) {
            throw new NullPointerException("other");
        }

        if (!fileId.equals(other.fileId)) {
            return fileId.getId() < other.fileId.getId() ? -1 : 1;
        }

        return HfsPlusUtilities.fastUnicodeCompare(name, other.name);
    }

    public String toString() {
        return name + " (" + fileId + ")";
    }
}
