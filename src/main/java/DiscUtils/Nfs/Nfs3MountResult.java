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


public final class Nfs3MountResult extends Nfs3CallResult {
    public Nfs3MountResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        if (getStatus() == Nfs3Status.Ok) {
            setFileHandle(new Nfs3FileHandle(reader));
            int numAuthFlavours = reader.readInt32();
            setAuthFlavours(new ArrayList<RpcAuthFlavour>(numAuthFlavours));
            for (int i = 0; i < numAuthFlavours; ++i) {
                getAuthFlavours().add(RpcAuthFlavour.values()[reader.readInt32()]);
            }
        } else {
            throw new Nfs3Exception(getStatus());
        }
    }

    public Nfs3MountResult() {
    }

    private List<RpcAuthFlavour> _authFlavours;

    public List<RpcAuthFlavour> getAuthFlavours() {
        return _authFlavours;
    }

    public void setAuthFlavours(List<RpcAuthFlavour> value) {
        _authFlavours = value;
    }

    private Nfs3FileHandle _fileHandle;

    public Nfs3FileHandle getFileHandle() {
        return _fileHandle;
    }

    public void setFileHandle(Nfs3FileHandle value) {
        _fileHandle = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_status.getValue());
        if (getStatus() == Nfs3Status.Ok) {
            getFileHandle().write(writer);
            writer.write(getAuthFlavours().size());
            for (int i = 0; i < getAuthFlavours().size(); i++) {
                writer.write(getAuthFlavours().get(i).ordinal());
            }
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3MountResult ? (Nfs3MountResult) obj : (Nfs3MountResult) null);
    }

    public boolean equals(Nfs3MountResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && other.getAuthFlavours().equals(getAuthFlavours()) &&
               other.getFileHandle().equals(getFileHandle());
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getStatus(), getFileHandle(), getAuthFlavours());
    }
}
