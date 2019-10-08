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

package DiscUtils.Nfs;

import moe.yo3explorer.dotnetio4j.MemoryStream;


public class RpcAuthentication {
    private byte[] _body;

    private RpcAuthFlavour _flavour;

    public RpcAuthentication() {
        _body = new byte[400];
    }

    public RpcAuthentication(XdrDataReader reader) {
        _flavour = RpcAuthFlavour.valueOf(reader.readInt32());
        _body = reader.readBuffer(400);
    }

    public RpcAuthentication(RpcCredentials credential) {
        _flavour = credential.getAuthFlavour();
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = new XdrDataWriter(ms);
        credential.write(writer);
        _body = ms.toArray();
    }

    public static RpcAuthentication null_() {
        return new RpcAuthentication(new RpcNullCredentials());
    }

    public void write(XdrDataWriter writer) {
        writer.write(_flavour.ordinal());
        writer.writeBuffer(_body);
    }

    public boolean equals(Object obj) {
        try {
            return equals(obj instanceof RpcAuthentication ? (RpcAuthentication) obj : (RpcAuthentication) null);
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    public boolean equals(RpcAuthentication other) {
        try {
            if (other == null) {
                return false;
            }

            return other._flavour == _flavour;
        } catch (RuntimeException __dummyCatchVar1) {
            throw __dummyCatchVar1;
        } catch (Exception __dummyCatchVar1) {
            throw new RuntimeException(__dummyCatchVar1);
        }

    }

    public int hashCode() {
        return _flavour.hashCode();
    }
}
