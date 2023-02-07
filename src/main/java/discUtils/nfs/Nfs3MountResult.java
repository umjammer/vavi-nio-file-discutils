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


public final class Nfs3MountResult extends Nfs3CallResult {

    public Nfs3MountResult(XdrDataReader reader) {
        status = Nfs3Status.valueOf(reader.readInt32());
        if (status == Nfs3Status.Ok) {
            fileHandle = new Nfs3FileHandle(reader);
            int numAuthFlavours = reader.readInt32();
            authFlavours = new ArrayList<>(numAuthFlavours);
            for (int i = 0; i < numAuthFlavours; ++i) {
                authFlavours.add(RpcAuthFlavour.values()[reader.readInt32()]);
            }
        } else {
            throw new Nfs3Exception(status);
        }
    }

    public Nfs3MountResult() {
    }

    private List<RpcAuthFlavour> authFlavours;

    public List<RpcAuthFlavour> getAuthFlavours() {
        return authFlavours;
    }

    public void setAuthFlavours(List<RpcAuthFlavour> value) {
        authFlavours = value;
    }

    private Nfs3FileHandle fileHandle;

    public Nfs3FileHandle getFileHandle() {
        return fileHandle;
    }

    public void setFileHandle(Nfs3FileHandle value) {
        fileHandle = value;
    }

    @Override public void write(XdrDataWriter writer) {
        writer.write(status.getValue());
        if (status == Nfs3Status.Ok) {
            fileHandle.write(writer);
            writer.write(authFlavours.size());
            for (RpcAuthFlavour authFlavour : authFlavours) {
                writer.write(authFlavour.ordinal());
            }
        }
    }

    @Override public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3MountResult ? (Nfs3MountResult) obj : null);
    }

    public boolean equals(Nfs3MountResult other) {
        if (other == null) {
            return false;
        }

        return other.status == status && other.authFlavours.equals(authFlavours) &&
               other.fileHandle.equals(fileHandle);
    }

    @Override public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(status, fileHandle, authFlavours);
    }
}
