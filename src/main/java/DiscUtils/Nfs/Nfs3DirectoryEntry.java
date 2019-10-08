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

package DiscUtils.Nfs;

import java.io.IOException;

import DiscUtils.Core.Internal.Utilities;
import moe.yo3explorer.dotnetio4j.MemoryStream;


public final class Nfs3DirectoryEntry {
    public Nfs3DirectoryEntry(XdrDataReader reader) {
        setFileId(reader.readUInt64());
        setName(reader.readString());
        setCookie(reader.readUInt64());
        if (reader.readBool()) {
            setFileAttributes(new Nfs3FileAttributes(reader));
        }

        if (reader.readBool()) {
            setFileHandle(new Nfs3FileHandle(reader));
        }
    }

    public Nfs3DirectoryEntry() {
    }

    private long __Cookie;

    public long getCookie() {
        return __Cookie;
    }

    public void setCookie(long value) {
        __Cookie = value;
    }

    private Nfs3FileAttributes __FileAttributes;

    public Nfs3FileAttributes getFileAttributes() {
        return __FileAttributes;
    }

    public void setFileAttributes(Nfs3FileAttributes value) {
        __FileAttributes = value;
    }

    private Nfs3FileHandle __FileHandle;

    public Nfs3FileHandle getFileHandle() {
        return __FileHandle;
    }

    public void setFileHandle(Nfs3FileHandle value) {
        __FileHandle = value;
    }

    private long __FileId;

    public long getFileId() {
        return __FileId;
    }

    public void setFileId(long value) {
        __FileId = value;
    }

    private String __Name;

    public String getName() {
        return __Name;
    }

    public void setName(String value) {
        __Name = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getFileId());
        writer.write(getName());
        writer.write(getCookie());
        writer.write(getFileAttributes() != null);
        if (getFileAttributes() != null) {
            getFileAttributes().write(writer);
        }

        writer.write(getFileHandle() != null);
        if (getFileHandle() != null) {
            getFileHandle().write(writer);
        }

    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3DirectoryEntry ? (Nfs3DirectoryEntry) obj : (Nfs3DirectoryEntry) null);
    }

    public boolean equals(Nfs3DirectoryEntry other) {
        if (other == null) {
            return false;
        }

        return other.getCookie() == getCookie() && other.getFileAttributes().equals(getFileAttributes()) &&
               other.getFileHandle().equals(getFileHandle()) && other.getFileId() == getFileId() &&
               other.getName().equals(getName());
    }

    public int hashCode() {
        return Utilities.getCombinedHashCode(Long.hashCode(getCookie()),
                                          getFileAttributes().hashCode(),
                                          getFileHandle().hashCode(),
                                          Long.hashCode(getFileId()),
                                          getName().hashCode());
    }

    public String toString() {
        return this.getName();
    }

    public long getSize() {
        MemoryStream stream = new MemoryStream();
        try {
            XdrDataWriter writer = new XdrDataWriter(stream);
            write(writer);
            return stream.getLength();
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

}
