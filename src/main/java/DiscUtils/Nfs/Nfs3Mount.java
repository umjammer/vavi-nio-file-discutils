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
// For more information, see
// https://www.ietf.org/rfc/rfc1813.txt Appendix I: Mount Protocol

package DiscUtils.Nfs;

import java.util.ArrayList;
import java.util.List;

import moe.yo3explorer.dotnetio4j.MemoryStream;


public final class Nfs3Mount extends RpcProgram {
    public static final int ProgramIdentifier = RpcIdentifiers.Nfs3MountProgramIdentifier;

    public static final int ProgramVersion = RpcIdentifiers.Nfs3MountProgramVersion;

    public static final int MaxPathLength = 1024;

    public static final int MaxNameLength = 255;

    public static final int MaxFileHandleSize = 64;

    public Nfs3Mount(IRpcClient client) {
        super(client);
    }

    public int getIdentifier() {
        return ProgramIdentifier;
    }

    public int getVersion() {
        return ProgramVersion;
    }

    public List<Nfs3Export> exports() {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, null, MountProc3.Export);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            List<Nfs3Export> exports = new ArrayList<>();
            while (reply.getBodyReader().readBool()) {
                exports.add(new Nfs3Export(reply.getBodyReader()));
            }
            return exports;
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3MountResult mount(String dirPath) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), MountProc3.Mnt);
        writer.write(dirPath);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3MountResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }
}
