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

package DiscUtils.SquashFs;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class DirectoryRecord implements IByteArraySerializable {
    public short InodeNumber;

    public String Name;

    public short Offset;

    public InodeType Type/* = InodeType.Directory*/;

    public long getSize() {
        return 8 + Name.length();
    }

    public int readFrom(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(Offset, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(InodeNumber, buffer, offset + 2);
        EndianUtilities.writeBytesLittleEndian((short) Type.ordinal(), buffer, offset + 4);
        EndianUtilities.writeBytesLittleEndian((short) (Name.length() - 1), buffer, offset + 6);
        EndianUtilities.stringToBytes(Name, buffer, offset + 8, Name.length());
    }

    public static DirectoryRecord readFrom(MetablockReader reader) {
        DirectoryRecord result = new DirectoryRecord();
        result.Offset = reader.readUShort();
        result.InodeNumber = reader.readShort();
        result.Type = InodeType.valueOf(reader.readUShort());
        short size = reader.readUShort();
        result.Name = reader.readString(size + 1);
System.err.println(result.Name);
        return result;
    }
}
