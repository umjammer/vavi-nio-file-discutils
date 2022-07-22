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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import discUtils.core.IDiagnosticTraceable;
import discUtils.streams.IByteArraySerializable;


public class AttributeList implements IByteArraySerializable, IDiagnosticTraceable, Iterable<AttributeListRecord> {
    private final List<AttributeListRecord> records;

    public AttributeList() {
        records = new ArrayList<>();
    }

    public int size() {
        int total = 0;
        for (AttributeListRecord record : records) {
            total += record.size();
        }
        return total;
    }

    public int readFrom(byte[] buffer, int offset) {
        records.clear();
        int pos = 0;
        while (pos < buffer.length) {
            AttributeListRecord r = new AttributeListRecord();
            pos += r.readFrom(buffer, offset + pos);
            records.add(r);
        }
        return pos;
    }

    public void writeTo(byte[] buffer, int offset) {
        int pos = offset;
        for (AttributeListRecord record : records) {
            record.writeTo(buffer, offset + pos);
            pos += record.size();
        }
    }

    public int getCount() {
        return records.size();
    }

    public boolean isReadOnly() {
        return false;
    }

    public void add(AttributeListRecord item) {
        records.add(item);
        Collections.sort(records);
    }

    public void clear() {
        records.clear();
    }

    public boolean contains(AttributeListRecord item) {
        return records.contains(item);
    }

    public void copyTo(List<AttributeListRecord> array, int arrayIndex) {
        array.addAll(arrayIndex, records);
    }

    public boolean remove(AttributeListRecord item) {
        return records.remove(item);
    }

    public Enumeration<AttributeListRecord> enumerator() {
        return Collections.enumeration(records);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "ATTRIBUTE LIST RECORDS");
        for (AttributeListRecord r : records) {
            r.dump(writer, indent + "  ");
        }
    }

    @Override
    public Iterator<AttributeListRecord> iterator() {
        return records.iterator();
    }
}
