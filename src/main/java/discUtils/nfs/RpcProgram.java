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

import dotnet4j.io.MemoryStream;


public abstract class RpcProgram {

    public static final int RpcVersion = 2;

    protected final IRpcClient client;

    protected RpcProgram(IRpcClient client) {
        this.client = client;
    }

    public abstract int getIdentifier();

    public abstract int getVersion();

    public void nullProc() {
        MemoryStream ms = new MemoryStream();
        @SuppressWarnings("unused")
        XdrDataWriter writer = startCallMessage(ms, null, NfsProc3.Null);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
        } else {
            throw new RpcException(reply.getHeader().getReplyHeader());
        }
    }

    protected RpcReply doSend(MemoryStream ms) {
        IRpcTransport transport = client.getTransport(getIdentifier(), getVersion());
        byte[] buffer = ms.toArray();
        buffer = transport.sendAndReceive(buffer);
        XdrDataReader reader = new XdrDataReader(new MemoryStream(buffer));
        RpcMessageHeader header = new RpcMessageHeader(reader);
        return new RpcReply() {{ setHeader(header); setBodyReader(reader); }};
    }

    protected XdrDataWriter startCallMessage(MemoryStream ms, RpcCredentials credentials, PortMapProc2 procedure) {
        return startCallMessage(ms, credentials, procedure.ordinal());
    }

    protected XdrDataWriter startCallMessage(MemoryStream ms, RpcCredentials credentials, MountProc3 procedure) {
        return startCallMessage(ms, credentials, procedure.ordinal());
    }

    protected XdrDataWriter startCallMessage(MemoryStream ms, RpcCredentials credentials, NfsProc3 procedure) {
        return startCallMessage(ms, credentials, procedure.ordinal());
    }

    protected XdrDataWriter startCallMessage(MemoryStream ms, RpcCredentials credentials, int procedure) {
        XdrDataWriter writer = new XdrDataWriter(ms);
        writer.write(client.nextTransactionId());
        writer.write(RpcMessageType.Call.ordinal());
        RpcCallHeader hdr = new RpcCallHeader();
        hdr.setRpcVersion(RpcVersion);
        hdr.setProgram(getIdentifier());
        hdr.setVersion(getVersion());
        hdr.setProc(procedure);
        hdr.setCredentials(new RpcAuthentication(credentials != null ? credentials : new RpcNullCredentials()));
        hdr.setVerifier(RpcAuthentication.null_());
        hdr.write(writer);
        return writer;
    }
}