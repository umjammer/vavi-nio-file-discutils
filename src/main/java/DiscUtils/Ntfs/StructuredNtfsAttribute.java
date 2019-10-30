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

package DiscUtils.Ntfs;

import java.io.IOException;
import java.io.PrintWriter;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


public class StructuredNtfsAttribute<T extends IByteArraySerializable & IDiagnosticTraceable> extends NtfsAttribute {
    private boolean _hasContent;

    private boolean _initialized;

    private T _structure;

    public StructuredNtfsAttribute(Class<T> clazz, File file, FileRecordReference containingFile, AttributeRecord record) {
        super(file, containingFile, record);
        try {
            _structure = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public T getContent() {
        initialize();
        return _structure;
    }

    public void setContent(T value) {
        _structure = value;
        _hasContent = true;
    }

    public boolean getHasContent() {
        initialize();
        return _hasContent;
    }

    public void save() {
        try (Stream s = open(FileAccess.Write)) {
            byte[] buffer = new byte[_structure.size()];
            _structure.writeTo(buffer, 0);
            s.write(buffer, 0, buffer.length);
            s.setLength(buffer.length);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public String toString() {
        initialize();
        return _structure.toString();
    }

    public void dump(PrintWriter writer, String indent) {
        initialize();
        writer.println(indent + getAttributeTypeName() + " ATTRIBUTE (" + (getName() == null ? "No Name" : getName()) + ")");
        _structure.dump(writer, indent + "  ");
        _primaryRecord.dump(writer, indent + "  ");
    }

    private void initialize() {
        if (!_initialized) {
            try (Stream s = open(FileAccess.Read)) {
                {
                    byte[] buffer = StreamUtilities.readExact(s, (int) getLength());
                    _structure.readFrom(buffer, 0);
                    _hasContent = s.getLength() != 0;
                }
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
            _initialized = true;
        }
    }
}
