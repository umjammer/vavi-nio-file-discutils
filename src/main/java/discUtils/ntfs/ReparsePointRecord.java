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

package discUtils.ntfs;

import java.io.PrintWriter;

import discUtils.core.IDiagnosticTraceable;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public final class ReparsePointRecord implements IByteArraySerializable, IDiagnosticTraceable {

    public byte[] content;

    public int tag;

    public int size() {
        return 8 + content.length;
    }

    public int readFrom(byte[] buffer, int offset) {
        tag = EndianUtilities.toUInt32LittleEndian(buffer, offset);
        short length = EndianUtilities.toUInt16LittleEndian(buffer, offset + 4);
        content = new byte[length];
        System.arraycopy(buffer, offset + 8, content, 0, length);
        return 8 + length;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(tag, buffer, offset);
        EndianUtilities.writeBytesLittleEndian((short) content.length, buffer, offset + 4);
        EndianUtilities.writeBytesLittleEndian((short) 0, buffer, offset + 6);
        System.arraycopy(content, 0, buffer, offset + 8, content.length);
    }

    public void dump(PrintWriter writer, String linePrefix) {
        writer.println(linePrefix + "                Tag: " + Integer.toHexString(tag));
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(content.length, 32); ++i) {
            hex.append(String.format(" %02x", content[i]));
        }
        writer.println(linePrefix + "               Data:" + hex + (content.length > 32 ? "..." : ""));
    }
}
