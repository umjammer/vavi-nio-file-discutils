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

package discUtils.squashFs;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


public class DirectoryRecord implements IByteArraySerializable {

    public short inodeNumber;

    public String name;

    private short offset;

    public int getOffset() {
        return offset & 0xffff;
    }

    public void setOffset(short value) {
        offset = value;
    }

    public InodeType type;

    @Override public int size() {
        return 8 + name.length();
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeShort(this.offset, buffer, offset + 0);
        ByteUtil.writeLeShort(inodeNumber, buffer, offset + 2);
        ByteUtil.writeLeShort((short) type.ordinal(), buffer, offset + 4);
        ByteUtil.writeLeShort((short) (name.length() - 1), buffer, offset + 6);
        EndianUtilities.stringToBytes(name, buffer, offset + 8, name.length());
    }

    public static DirectoryRecord readFrom(MetablockReader reader) {
        DirectoryRecord result = new DirectoryRecord();
        result.offset = reader.readUShort();
        result.inodeNumber = reader.readShort();
        result.type = InodeType.values()[reader.readUShort()];
        short size = reader.readUShort();
        result.name = reader.readString(size + 1);
//logger.log(Level.DEBUG, result.Name);
        return result;
    }
}
