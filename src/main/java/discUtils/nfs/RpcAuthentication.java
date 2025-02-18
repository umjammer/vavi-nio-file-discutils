//
// Copyright (c) 2008-2011, Kenneth Bell
// Copyright (c) 2017, Quamotion
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

import dotnet4j.io.MemoryStream;


public class RpcAuthentication {

    private byte[] body;

    private RpcAuthFlavour flavour = RpcAuthFlavour.Null;

    public RpcAuthentication() {
        body = new byte[400];
    }

    public RpcAuthentication(XdrDataReader reader) {
        flavour = RpcAuthFlavour.values()[reader.readInt32()];
        body = reader.readBuffer(400);
    }

    public RpcAuthentication(RpcCredentials credential) {
        flavour = credential.getAuthFlavour();
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = new XdrDataWriter(ms);
        credential.write(writer);
        body = ms.toArray();
    }

    public static RpcAuthentication null_() {
        return new RpcAuthentication(new RpcNullCredentials());
    }

    public void write(XdrDataWriter writer) {
        writer.write(flavour.ordinal());
        writer.writeBuffer(body);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof RpcAuthentication ? (RpcAuthentication) obj : null);
    }

    public boolean equals(RpcAuthentication other) {
        if (other == null) {
            return false;
        }

        return other.flavour == flavour;
    }

    public int hashCode() {
        return flavour.hashCode();
    }
}
