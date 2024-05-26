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

package discUtils.hfsPlus;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


final class ExtentKey extends BTreeKey<ExtentKey> implements XComparable<ExtentKey> {

    private static final Logger logger = getLogger(ExtentKey.class.getName());

    // 0 is data, 0xff is rsrc
    private byte forkType;

    private short keyLength;

    private int startBlock;

    public ExtentKey() {
    }

    public ExtentKey(CatalogNodeId cnid, int startBlock, boolean resource_fork) {
        keyLength = 10;
        nodeId = cnid;
        this.startBlock = startBlock;
        forkType = (byte) (resource_fork ? 0xff : 0x00);
    }

    private CatalogNodeId nodeId;

    public CatalogNodeId getNodeId() {
        return nodeId;
    }

    public void setNodeId(CatalogNodeId value) {
        nodeId = value;
    }

    @Override public int size() {
        return 12;
    }

    @Override public int compareTo(ExtentKey other) {
        if (other == null) {
            throw new NullPointerException("other");
        }

        // Sort by file id, fork type, then starting block
        if (!nodeId.equals(other.nodeId)) {
            return nodeId.getId() < other.nodeId.getId() ? -1 : 1;
        }

        if (forkType != other.forkType) {
            return forkType < other.forkType ? -1 : 1;
        }

        if (startBlock != other.startBlock) {
            return startBlock < other.startBlock ? -1 : 1;
        }

        return 0;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        keyLength = ByteUtil.readBeShort(buffer, offset + 0);
        forkType = buffer[offset + 2];
        nodeId = new CatalogNodeId(ByteUtil.readBeInt(buffer, offset + 4));
        startBlock = ByteUtil.readBeInt(buffer, offset + 8);
logger.log(Level.DEBUG, (keyLength & 0xffff) + 2);
        return (keyLength & 0xffff) + 2;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override public int compareTo(BTreeKey<?> other) {
        return compareTo(other instanceof ExtentKey ? (ExtentKey) other : null);
    }

    @Override public String toString() {
        return "ExtentKey (" + nodeId + " - " + startBlock + ")";
    }
}
