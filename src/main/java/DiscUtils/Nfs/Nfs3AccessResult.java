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

import java.util.EnumSet;


public final class Nfs3AccessResult extends Nfs3CallResult {
    public Nfs3AccessResult() {
    }

    public Nfs3AccessResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (reader.readBool()) {
            setObjectAttributes(new Nfs3FileAttributes(reader));
        }

        setAccess(Nfs3AccessPermissions.valueOf(reader.readInt32()));
    }

    private EnumSet<Nfs3AccessPermissions> __Access;

    public EnumSet<Nfs3AccessPermissions> getAccess() {
        return __Access;
    }

    public void setAccess(EnumSet<Nfs3AccessPermissions> value) {
        __Access = value;
    }

    private Nfs3FileAttributes __ObjectAttributes;

    public Nfs3FileAttributes getObjectAttributes() {
        return __ObjectAttributes;
    }

    public void setObjectAttributes(Nfs3FileAttributes value) {
        __ObjectAttributes = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getStatus().getValue());
        writer.write(getObjectAttributes() != null);
        if (getObjectAttributes() != null) {
            getObjectAttributes().write(writer);
        }

        writer.write((int) Nfs3AccessPermissions.valueOf(getAccess()));
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3AccessResult ? (Nfs3AccessResult) obj : (Nfs3AccessResult) null);
    }

    public boolean equals(Nfs3AccessResult other) {
        if (other == null) {
            return false;
        }

        return other.getAccess().equals(getAccess()) && dotnet4j.io.compat.Utilities.equals(other.getObjectAttributes(), getObjectAttributes())
                && other.getStatus() == getStatus();
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getAccess(), getObjectAttributes(), getStatus());
    }
}
