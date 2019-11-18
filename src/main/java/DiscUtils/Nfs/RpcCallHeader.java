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

    private RpcAuthentication _credentials;

    public RpcAuthentication getCredentials() {
        return _credentials;
    }

    public void setCredentials(RpcAuthentication value) {
        _credentials = value;
    }

    private int _proc;

    public int getProc() {
        return _proc;
    }

    public void setProc(int value) {
        _proc = value;
    }

    private int _program;

    public int getProgram() {
        return _program;
    }

    public void setProgram(int value) {
        _program = value;
    }

    private int _rpcVersion;

    public int getRpcVersion() {
        return _rpcVersion;
    }

    public void setRpcVersion(int value) {
        _rpcVersion = value;
    }

    private RpcAuthentication _verifier;

    public RpcAuthentication getVerifier() {
        return _verifier;
    }

    public void setVerifier(RpcAuthentication value) {
        _verifier = value;
    }

    private int _version;

    public int getVersion() {
        return _version;
    }

    public void setVersion(int value) {
        _version = value;
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
