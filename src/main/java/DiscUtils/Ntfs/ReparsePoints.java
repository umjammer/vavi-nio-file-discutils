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
import java.util.Map;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class ReparsePoints {
    private final File _file;

    private final IndexView<DiscUtils.Ntfs.ReparsePoints.Key, DiscUtils.Ntfs.ReparsePoints.Data> _index;

    public ReparsePoints(File file) {
        _file = file;
        _index = new IndexView<>(file.getIndex("$R"));
    }

    public void add(int tag, FileRecordReference file) {
        DiscUtils.Ntfs.ReparsePoints.Key newKey = new DiscUtils.Ntfs.ReparsePoints.Key();
        newKey.Tag = tag;
        newKey.File = file;
        DiscUtils.Ntfs.ReparsePoints.Data data = new DiscUtils.Ntfs.ReparsePoints.Data();
        _index.set___idx(newKey, data);
        _file.updateRecordInMft();
    }

    public void remove(int tag, FileRecordReference file) {
        DiscUtils.Ntfs.ReparsePoints.Key key = new DiscUtils.Ntfs.ReparsePoints.Key();
        key.Tag = tag;
        key.File = file;
        _index.remove(key);
        _file.updateRecordInMft();
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "REPARSE POINT INDEX");
        for (Map.Entry<DiscUtils.Ntfs.ReparsePoints.Key, DiscUtils.Ntfs.ReparsePoints.Data> entry : _index.getEntries()
                .entrySet()) {
            writer.println(indent + "  REPARSE POINT INDEX ENTRY");
            writer.println(indent + "            Tag: " + String.format("%02x", entry.getKey().Tag));
            writer.println(indent + "  MFT Reference: " + entry.getKey().File);
        }
    }

    public final static class Key implements IByteArraySerializable {
        public FileRecordReference File = new FileRecordReference();

        public int Tag;

        public int sizeOf() {
            return 12;
        }

        public int readFrom(byte[] buffer, int offset) {
            Tag = EndianUtilities.toUInt32LittleEndian(buffer, offset);
            File = new FileRecordReference(EndianUtilities.toUInt64LittleEndian(buffer, offset + 4));
            return 12;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(Tag, buffer, offset);
            EndianUtilities.writeBytesLittleEndian(File.getValue(), buffer, offset + 4);
        }

        /**
         * /Utilities.WriteBytesLittleEndian((int)0, buffer, offset + 12);
         */
        public String toString() {
            return String.format("%x:", Tag) + File;
        }
    }

    public final static class Data implements IByteArraySerializable {
        public int sizeOf() {
            return 0;
        }

        public int readFrom(byte[] buffer, int offset) {
            return 0;
        }

        public void writeTo(byte[] buffer, int offset) {
        }

        public String toString() {
            return "<no data>";
        }
    }
}
