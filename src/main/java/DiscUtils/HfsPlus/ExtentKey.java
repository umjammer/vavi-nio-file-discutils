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

import vavi.util.Debug;

import DiscUtils.Streams.Util.EndianUtilities;


final class ExtentKey extends BTreeKey<ExtentKey> implements XComparable<ExtentKey> {
    // 0 is data, 0xff is rsrc
    private byte _forkType;

    private short _keyLength;

    private int _startBlock;

    public ExtentKey() {
    }

    public ExtentKey(CatalogNodeId cnid, int startBlock, boolean resource_fork) {
        _keyLength = 10;
        _nodeId = cnid;
        _startBlock = startBlock;
        _forkType = (byte) (resource_fork ? 0xff : 0x00);
    }

    private CatalogNodeId _nodeId;

    public CatalogNodeId getNodeId() {
        return _nodeId;
    }

    public void setNodeId(CatalogNodeId value) {
        _nodeId = value;
    }

    public int size() {
        return 12;
    }

    public int compareTo(ExtentKey other) {
        if (other == null) {
            throw new NullPointerException("other");
        }

        // Sort by file id, fork type, then starting block
        if (!_nodeId.equals(other._nodeId)) {
            return _nodeId.getId() < other._nodeId.getId() ? -1 : 1;
        }

        if (_forkType != other._forkType) {
            return _forkType < other._forkType ? -1 : 1;
        }

        if (_startBlock != other._startBlock) {
            return _startBlock < other._startBlock ? -1 : 1;
        }

        return 0;
    }

    public int readFrom(byte[] buffer, int offset) {
        _keyLength = EndianUtilities.toUInt16BigEndian(buffer, offset + 0);
        _forkType = buffer[offset + 2];
        _nodeId = new CatalogNodeId(EndianUtilities.toUInt32BigEndian(buffer, offset + 4));
        _startBlock = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
Debug.println((_keyLength & 0xffff) + 2);
        return (_keyLength & 0xffff) + 2;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public int compareTo(BTreeKey<?> other) {
        return compareTo(other instanceof ExtentKey ? (ExtentKey) other : (ExtentKey) null);
    }

    public String toString() {
        return "ExtentKey (" + _nodeId + " - " + _startBlock + ")";
    }
}
