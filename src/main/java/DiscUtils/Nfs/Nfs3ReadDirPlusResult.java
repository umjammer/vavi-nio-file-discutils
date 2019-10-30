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

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.Internal.Utilities;


public final class Nfs3ReadDirPlusResult extends Nfs3CallResult {
    public Nfs3ReadDirPlusResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (reader.readBool()) {
            setDirAttributes(new Nfs3FileAttributes(reader));
        }

        if (getStatus() == Nfs3Status.Ok) {
            setCookieVerifier(reader.readUInt64());
            setDirEntries(new ArrayList<Nfs3DirectoryEntry>());
            while (reader.readBool()) {
                Nfs3DirectoryEntry dirEntry = new Nfs3DirectoryEntry(reader);
                getDirEntries().add(dirEntry);
            }
            setEof(reader.readBool());
        }
    }

    public Nfs3ReadDirPlusResult() {
    }

    private long __CookieVerifier;

    public long getCookieVerifier() {
        return __CookieVerifier;
    }

    public void setCookieVerifier(long value) {
        __CookieVerifier = value;
    }

    private Nfs3FileAttributes __DirAttributes;

    public Nfs3FileAttributes getDirAttributes() {
        return __DirAttributes;
    }

    public void setDirAttributes(Nfs3FileAttributes value) {
        __DirAttributes = value;
    }

    private List<Nfs3DirectoryEntry> __DirEntries;

    public List<Nfs3DirectoryEntry> getDirEntries() {
        return __DirEntries;
    }

    public void setDirEntries(List<Nfs3DirectoryEntry> value) {
        __DirEntries = value;
    }

    private boolean __Eof;

    public boolean getEof() {
        return __Eof;
    }

    public void setEof(boolean value) {
        __Eof = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getStatus().ordinal());
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
        return equals(obj instanceof Nfs3ReadDirPlusResult ? (Nfs3ReadDirPlusResult) obj : (Nfs3ReadDirPlusResult) null);
    }

    public boolean equals(Nfs3ReadDirPlusResult other) {
        try {
            if (other == null) {
                return false;
            }

            return other.getStatus() == getStatus() && other.getDirAttributes().equals(getDirAttributes()) &&
                   other.getDirEntries().equals(getDirEntries()) && other.getCookieVerifier() == getCookieVerifier();
        } catch (RuntimeException __dummyCatchVar1) {
            throw __dummyCatchVar1;
        } catch (Exception __dummyCatchVar1) {
            throw new RuntimeException(__dummyCatchVar1);
        }

    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getStatus(), getDirAttributes(), getCookieVerifier(), getDirEntries());
    }
}
