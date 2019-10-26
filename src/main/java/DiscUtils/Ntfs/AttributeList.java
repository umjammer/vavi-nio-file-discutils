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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.IByteArraySerializable;


public class AttributeList implements IByteArraySerializable, IDiagnosticTraceable, Iterable<AttributeListRecord> {
    private final List<AttributeListRecord> _records;

    public AttributeList() {
        _records = new ArrayList<>();
    }

    public int size() {
        int total = 0;
        for (AttributeListRecord record : _records) {
            total += record.size();
        }
        return total;
    }

    public int readFrom(byte[] buffer, int offset) {
        _records.clear();
        int pos = 0;
        while (pos < buffer.length) {
            AttributeListRecord r = new AttributeListRecord();
            pos += r.readFrom(buffer, offset + pos);
            _records.add(r);
        }
        return pos;
    }

    public void writeTo(byte[] buffer, int offset) {
        int pos = offset;
        for (AttributeListRecord record : _records) {
            record.writeTo(buffer, offset + pos);
            pos += record.size();
        }
    }

    public int getCount() {
        return _records.size();
    }

    public boolean isReadOnly() {
        return false;
    }

    public void add(AttributeListRecord item) {
        _records.add(item);
        Collections.sort(_records);
    }

    public void clear() {
        _records.clear();
    }

    public boolean contains(AttributeListRecord item) {
        return _records.contains(item);
    }

    public void copyTo(List<AttributeListRecord> array, int arrayIndex) {
        array.addAll(arrayIndex, _records);
    }

    public boolean remove(AttributeListRecord item) {
        return _records.remove(item);
    }

    public Enumeration<AttributeListRecord> getEnumerator() {
        return Collections.enumeration(_records);
    }

    Enumeration<?> iEnumerable___GetEnumerator() {
        return Collections.enumeration(_records);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "ATTRIBUTE LIST RECORDS");
        for (AttributeListRecord r : _records) {
            r.dump(writer, indent + "  ");
        }
    }

    @Override
    public Iterator<AttributeListRecord> iterator() {
        return _records.iterator();
    }
}
