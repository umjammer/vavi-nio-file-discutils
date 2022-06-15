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

import dotnet4j.io.MemoryStream;


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

    private long cookie;

    public long getCookie() {
        return cookie;
    }

    public void setCookie(long value) {
        cookie = value;
    }

    private Nfs3FileAttributes fileAttributes;

    public Nfs3FileAttributes getFileAttributes() {
        return fileAttributes;
    }

    public void setFileAttributes(Nfs3FileAttributes value) {
        fileAttributes = value;
    }

    private Nfs3FileHandle fileHandle;

    public Nfs3FileHandle getFileHandle() {
        return fileHandle;
    }

    public void setFileHandle(Nfs3FileHandle value) {
        fileHandle = value;
    }

    private long fileId;

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long value) {
        fileId = value;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
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
        return equals(obj instanceof Nfs3DirectoryEntry ? (Nfs3DirectoryEntry) obj : null);
    }

    public boolean equals(Nfs3DirectoryEntry other) {
        if (other == null) {
            return false;
        }

        return other.getCookie() == getCookie() && dotnet4j.io.compat.Utilities.equals(other.getFileAttributes(), getFileAttributes())
                && dotnet4j.io.compat.Utilities.equals(other.getFileHandle(), getFileHandle()) && other.getFileId() == getFileId()
                && other.getName().equals(getName());
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(Long.hashCode(getCookie()),
                                             getFileAttributes().hashCode(),
                                             getFileHandle().hashCode(),
                                             Long.hashCode(getFileId()),
                                             getName().hashCode());
    }

    public String toString() {
        return this.getName();
    }

    public long getSize() {
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            write(writer);
            return stream.getLength();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }
}
