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

package discUtils.udf;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public abstract class PartitionMap implements IByteArraySerializable {

    public byte type;

    @Override public abstract int size();

    @Override public int readFrom(byte[] buffer, int offset) {
        type = buffer[offset];
        return parse(buffer, offset);
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public static PartitionMap createFrom(byte[] buffer, int offset) {
        PartitionMap result = null;
        byte type = buffer[offset];
        if (type == 1) {
            result = new Type1PartitionMap();
        } else if (type == 2) {
            EntityIdentifier id = EndianUtilities.toStruct(UdfEntityIdentifier.class, buffer, offset + 4);
            result = switch (id.identifier) {
                case "*UDF Virtual Partition" -> new VirtualPartitionMap();
                case "*UDF Sparable Partition" -> new SparablePartitionMap();
                case "*UDF Metadata Partition" -> new MetadataPartitionMap();
                default -> throw new IllegalArgumentException("Unrecognized partition map entity id: " + id);
            };
        }

        if (result != null) {
            result.readFrom(buffer, offset);
        }

        return result;
    }

    protected abstract int parse(byte[] buffer, int offset);
}
