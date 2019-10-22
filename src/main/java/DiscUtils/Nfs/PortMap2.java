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

import moe.yo3explorer.dotnetio4j.MemoryStream;


public final class PortMap2 extends RpcProgram {
    public static final int ProgramIdentifier = 100000;

    public static final int ProgramVersion = 2;

    public PortMap2(RpcClient client) {
        super(client);
    }

    public int getIdentifier() {
        return ProgramIdentifier;
    }

    public int getVersion() {
        return ProgramVersion;
    }

    public int getPort(int program, int version, PortMap2Protocol protocol) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, null, PortMapProc2.GetPort);
        new PortMap2Mapping().write(writer);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            @SuppressWarnings("unused")
            PortMap2Port port = new PortMap2Port(reply.getBodyReader());
            return reply.getBodyReader().readUInt32();
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }
}
