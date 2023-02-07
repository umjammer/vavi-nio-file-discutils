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
import java.nio.charset.StandardCharsets;

import discUtils.core.IDiagnosticTraceable;
import discUtils.streams.IByteArraySerializable;


public final class VolumeName implements IByteArraySerializable, IDiagnosticTraceable {

    public VolumeName() {
    }

    public VolumeName(String name) {
        setName(name);
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    @Override public int size() {
        return getName().getBytes(StandardCharsets.UTF_16LE).length;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        setName(new String(buffer, offset, buffer.length - offset, StandardCharsets.UTF_16LE));
        return buffer.length - offset;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        byte[] bytes = getName().getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(bytes, 0, buffer, offset, bytes.length);
    }

    @Override public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "  Volume Name: " + name);
    }
}
