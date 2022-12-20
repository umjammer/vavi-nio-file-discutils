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
        status = Nfs3Status.valueOf(reader.readInt32());
        if (reader.readBool()) {
            dirAttributes = new Nfs3FileAttributes(reader);
        }

        if (status == Nfs3Status.Ok) {
            cookieVerifier = reader.readUInt64();
            dirEntries = new ArrayList<>();
            while (reader.readBool()) {
                Nfs3DirectoryEntry dirEntry = new Nfs3DirectoryEntry(reader);
                dirEntries.add(dirEntry);
            }
            eof = reader.readBool();
        }
    }

    public Nfs3ReadDirPlusResult() {
    }

    private long cookieVerifier;

    public long getCookieVerifier() {
        return cookieVerifier;
    }

    public void setCookieVerifier(long value) {
        cookieVerifier = value;
    }

    private Nfs3FileAttributes dirAttributes;

    public Nfs3FileAttributes getDirAttributes() {
        return dirAttributes;
    }

    public void setDirAttributes(Nfs3FileAttributes value) {
        dirAttributes = value;
    }

    private List<Nfs3DirectoryEntry> dirEntries;

    public List<Nfs3DirectoryEntry> getDirEntries() {
        return dirEntries;
    }

    public void setDirEntries(List<Nfs3DirectoryEntry> value) {
        dirEntries = value;
    }

    private boolean eof;

    public boolean getEof() {
        return eof;
    }

    public void setEof(boolean value) {
        eof = value;
    }

    @Override  public void write(XdrDataWriter writer) {
        writer.write(status.getValue());
        writer.write(dirAttributes != null);
        if (dirAttributes != null) {
            dirAttributes.write(writer);
        }

        if (status == Nfs3Status.Ok) {
            writer.write(cookieVerifier);
            for (Nfs3DirectoryEntry entry : dirEntries) {
                writer.write(true);
                entry.write(writer);
            }
            writer.write(false);
            writer.write(eof);
        }
    }

    @Override public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3ReadDirPlusResult ? (Nfs3ReadDirPlusResult) obj : null);
    }

    public boolean equals(Nfs3ReadDirPlusResult other) {
        if (other == null) {
            return false;
        }

        return other.status == status && other.dirAttributes.equals(dirAttributes) &&
               other.dirEntries.equals(dirEntries) && other.cookieVerifier == cookieVerifier;
    }

    @Override public int hashCode() {
        return dotnet4j.util.compat.Utilities
                .getCombinedHashCode(status, dirAttributes, cookieVerifier, dirEntries);
    }
}
