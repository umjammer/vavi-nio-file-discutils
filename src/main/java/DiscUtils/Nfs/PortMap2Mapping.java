//
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

/**
 * A mapping of(program, version, network ID) to address
 *
 * The network identifier(r_netid): This is a string that represents a local
 * identification for a network. This is defined by a system administrator based
 * on local conventions, and cannot be depended on to have the same value on
 * every system.
 */
public class PortMap2Mapping {
    public PortMap2Mapping() {
    }

    public PortMap2Mapping(XdrDataReader reader) {
        setProgram(reader.readInt32());
        setVersion(reader.readInt32());
        setProtocol(PortMap2Protocol.valueOf(reader.readUInt32()));
        setPort(reader.readUInt32());
    }

    private int _program;

    public int getProgram() {
        return _program;
    }

    public void setProgram(int value) {
        _program = value;
    }

    private int _version;

    public int getVersion() {
        return _version;
    }

    public void setVersion(int value) {
        _version = value;
    }

    private PortMap2Protocol _protocol = PortMap2Protocol.Tcp;

    public PortMap2Protocol getProtocol() {
        return _protocol;
    }

    public void setProtocol(PortMap2Protocol value) {
        _protocol = value;
    }

    private int _port;

    public int getPort() {
        return _port;
    }

    public void setPort(int value) {
        _port = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getProgram());
        writer.write(getVersion());
        writer.write(getProtocol().getValue());
        writer.write(getPort());
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof PortMap2Mapping ? (PortMap2Mapping) obj : (PortMap2Mapping) null);
    }

    public boolean equals(PortMap2Mapping other) {
        if (other == null) {
            return false;
        }

        return other.getProgram() == getProgram() && other.getVersion() == getVersion() &&
               other.getProtocol() == getProtocol() && other.getPort() == getPort();
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(getProgram(), getVersion(), getProtocol(), getPort());
    }
}
