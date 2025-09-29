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

import discUtils.streams.IByteArraySerializable;
import dotnet4j.util.compat.Tuple;
import vavi.util.ByteUtil;


public class ReparsePoints {

    private final File file;

    private final IndexView<discUtils.ntfs.ReparsePoints.Key, discUtils.ntfs.ReparsePoints.Data> index;

    public ReparsePoints(File file) {
        this.file = file;
        index = new IndexView<>(discUtils.ntfs.ReparsePoints.Key.class,
                                 discUtils.ntfs.ReparsePoints.Data.class,
                                 file.getIndex("$R"));
    }

    public void add(int tag, FileRecordReference file) {
        discUtils.ntfs.ReparsePoints.Key newKey = new discUtils.ntfs.ReparsePoints.Key();
        newKey.tag = tag;
        newKey.file = file;
        discUtils.ntfs.ReparsePoints.Data data = new discUtils.ntfs.ReparsePoints.Data();
        index.put(newKey, data);
        this.file.updateRecordInMft();
    }

    public void remove(int tag, FileRecordReference file) {
        discUtils.ntfs.ReparsePoints.Key key = new discUtils.ntfs.ReparsePoints.Key();
        key.tag = tag;
        key.file = file;
        index.remove(key);
        this.file.updateRecordInMft();
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "REPARSE POINT INDEX");
        for (Tuple<discUtils.ntfs.ReparsePoints.Key, discUtils.ntfs.ReparsePoints.Data> entry : index.getEntries()) {
            writer.println(indent + "  REPARSE POINT INDEX ENTRY");
            writer.println(indent + "            Tag: " + "%02x".formatted(entry.getKey().tag));
            writer.println(indent + "  MFT Reference: " + entry.getKey().file);
        }
    }

    public final static class Key implements IByteArraySerializable {

        public FileRecordReference file = new FileRecordReference();

        public int tag;

        @Override public int size() {
            return 12;
        }

        @Override public int readFrom(byte[] buffer, int offset) {
            tag = ByteUtil.readLeInt(buffer, offset);
            file = new FileRecordReference(ByteUtil.readLeLong(buffer, offset + 4));
            return 12;
        }

        @Override public void writeTo(byte[] buffer, int offset) {
            ByteUtil.writeLeInt(tag, buffer, offset);
            ByteUtil.writeLeLong(file.getValue(), buffer, offset + 4);
        }

        /**
         * /utilities.WriteBytesLittleEndian((int)0, buffer, offset + 12);
         */
        public String toString() {
            return "%x:".formatted(tag) + file;
        }
    }

    public final static class Data implements IByteArraySerializable {

        @Override public int size() {
            return 0;
        }

        @Override public int readFrom(byte[] buffer, int offset) {
            return 0;
        }

        @Override public void writeTo(byte[] buffer, int offset) {
        }

        @Override public String toString() {
            return "<no data>";
        }
    }
}
