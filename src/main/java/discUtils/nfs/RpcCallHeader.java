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

    private RpcAuthentication credentials;

    public RpcAuthentication getCredentials() {
        return credentials;
    }

    public void setCredentials(RpcAuthentication value) {
        credentials = value;
    }

    private int proc;

    public int getProc() {
        return proc;
    }

    public void setProc(int value) {
        proc = value;
    }

    private int program;

    public int getProgram() {
        return program;
    }

    public void setProgram(int value) {
        program = value;
    }

    private int rpcVersion;

    public int getRpcVersion() {
        return rpcVersion;
    }

    public void setRpcVersion(int value) {
        rpcVersion = value;
    }

    private RpcAuthentication verifier;

    public RpcAuthentication getVerifier() {
        return verifier;
    }

    public void setVerifier(RpcAuthentication value) {
        verifier = value;
    }

    private int version;

    public int getVersion() {
        return version;
    }

    public void setVersion(int value) {
        version = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(rpcVersion);
        writer.write(program);
        writer.write(version);
        writer.write(proc);
        credentials.write(writer);
        verifier.write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof RpcCallHeader ? (RpcCallHeader) obj : null);
    }

    public boolean equals(RpcCallHeader other) {
        if (other == null) {
            return false;
        }

        return other.credentials.equals(credentials) && other.proc == proc &&
               other.program == program && other.rpcVersion == rpcVersion &&
               other.verifier.equals(verifier) && other.version == version;
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(credentials, proc, program, rpcVersion, verifier, version);
    }
}
