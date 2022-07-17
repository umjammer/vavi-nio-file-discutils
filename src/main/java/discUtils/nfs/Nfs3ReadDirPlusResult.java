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

package discUtils.nfs;

import java.util.ArrayList;
import java.util.List;


public final class Nfs3ReadDirPlusResult extends Nfs3CallResult {
    public Nfs3ReadDirPlusResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (reader.readBool()) {
            setDirAttributes(new Nfs3FileAttributes(reader));
        }

        if (getStatus() == Nfs3Status.Ok) {
            setCookieVerifier(reader.readUInt64());
            setDirEntries(new ArrayList<>());
            while (reader.readBool()) {
                Nfs3DirectoryEntry dirEntry = new Nfs3DirectoryEntry(reader);
                getDirEntries().add(dirEntry);
            }
            setEof(reader.readBool());
        }
    }

    public Nfs3ReadDirPlusResult() {
    }

    private long _cookieVerifier;

    public long getCookieVerifier() {
        return _cookieVerifier;
    }

    public void setCookieVerifier(long value) {
        _cookieVerifier = value;
    }

    private Nfs3FileAttributes _dirAttributes;

    public Nfs3FileAttributes getDirAttributes() {
        return _dirAttributes;
    }

    public void setDirAttributes(Nfs3FileAttributes value) {
        _dirAttributes = value;
    }

    private List<Nfs3DirectoryEntry> _dirEntries;

    public List<Nfs3DirectoryEntry> getDirEntries() {
        return _dirEntries;
    }

    public void setDirEntries(List<Nfs3DirectoryEntry> value) {
        _dirEntries = value;
    }

    private boolean _eof;

    public boolean getEof() {
        return _eof;
    }

    public void setEof(boolean value) {
        _eof = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_status.getValue());
        writer.write(getDirAttributes() != null);
        if (getDirAttributes() != null) {
            getDirAttributes().write(writer);
        }

        if (getStatus() == Nfs3Status.Ok) {
            writer.write(getCookieVerifier());
            for (Nfs3DirectoryEntry entry : getDirEntries()) {
                writer.write(true);
                entry.write(writer);
            }
            writer.write(false);
            writer.write(getEof());
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3ReadDirPlusResult ? (Nfs3ReadDirPlusResult) obj : null);
    }

    public boolean equals(Nfs3ReadDirPlusResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && other.getDirAttributes().equals(getDirAttributes()) &&
               other.getDirEntries().equals(getDirEntries()) && other.getCookieVerifier() == getCookieVerifier();
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities
                .getCombinedHashCode(getStatus(), getDirAttributes(), getCookieVerifier(), getDirEntries());
    }
}
