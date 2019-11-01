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

public class RpcCallHeader {
    public RpcCallHeader() {
    }

    public RpcCallHeader(XdrDataReader reader) {
        setRpcVersion(reader.readUInt32());
        setProgram(reader.readUInt32());
        setVersion(reader.readUInt32());
        setProc(reader.readInt32());
        setCredentials(new RpcAuthentication(reader));
        setVerifier(new RpcAuthentication(reader));
    }

    private RpcAuthentication __Credentials;

    public RpcAuthentication getCredentials() {
        return __Credentials;
    }

    public void setCredentials(RpcAuthentication value) {
        __Credentials = value;
    }

    private int __Proc;

    public int getProc() {
        return __Proc;
    }

    public void setProc(int value) {
        __Proc = value;
    }

    private int __Program;

    public int getProgram() {
        return __Program;
    }

    public void setProgram(int value) {
        __Program = value;
    }

    private int __RpcVersion;

    public int getRpcVersion() {
        return __RpcVersion;
    }

    public void setRpcVersion(int value) {
        __RpcVersion = value;
    }

    private RpcAuthentication __Verifier;

    public RpcAuthentication getVerifier() {
        return __Verifier;
    }

    public void setVerifier(RpcAuthentication value) {
        __Verifier = value;
    }

    private int __Version;

    public int getVersion() {
        return __Version;
    }

    public void setVersion(int value) {
        __Version = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getRpcVersion());
        writer.write(getProgram());
        writer.write(getVersion());
        writer.write(getProc());
        getCredentials().write(writer);
        getVerifier().write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof RpcCallHeader ? (RpcCallHeader) obj : (RpcCallHeader) null);
    }

    public boolean equals(RpcCallHeader other) {
        if (other == null) {
            return false;
        }

        return other.getCredentials().equals(getCredentials()) && other.getProc() == getProc() &&
               other.getProgram() == getProgram() && other.getRpcVersion() == getRpcVersion() &&
               other.getVerifier().equals(getVerifier()) && other.getVersion() == getVersion();
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getCredentials(), getProc(), getProgram(), getRpcVersion(), getVerifier(), getVersion());
    }
}
