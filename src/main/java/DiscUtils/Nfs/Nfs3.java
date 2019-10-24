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

import java.util.EnumSet;

import dotnet4j.io.MemoryStream;


public final class Nfs3 extends RpcProgram {
    public static final int ProgramIdentifier = RpcIdentifiers.Nfs3ProgramIdentifier;

    public static final int ProgramVersion = RpcIdentifiers.Nfs3ProgramVersion;

    public static final int MaxFileHandleSize = 64;

    public static final int CookieVerifierSize = 8;

    public static final int CreateVerifierSize = 8;

    public static final int WriteVerifierSize = 8;

    public Nfs3(IRpcClient client) {
        super(client);
    }

    public int getIdentifier() {
        return ProgramIdentifier;
    }

    public int getVersion() {
        return ProgramVersion;
    }

    public Nfs3GetAttributesResult getAttributes(Nfs3FileHandle handle) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.GetAttr);
        handle.write(writer);
        writer.write(false);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3GetAttributesResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3ModifyResult setAttributes(Nfs3FileHandle handle, Nfs3SetAttributes newAttributes) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.SetAttr);
        handle.write(writer);
        newAttributes.write(writer);
        writer.write(false);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3ModifyResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3LookupResult lookup(Nfs3FileHandle dir, String name) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Lookup);
        dir.write(writer);
        writer.write(name);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3LookupResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3AccessResult access(Nfs3FileHandle handle, EnumSet<Nfs3AccessPermissions> requested) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Access);
        handle.write(writer);
        writer.write((int) Nfs3AccessPermissions.valueOf(requested));
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3AccessResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3ReadResult read(Nfs3FileHandle handle, long position, int count) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Read);
        handle.write(writer);
        writer.write(position);
        writer.write(count);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3ReadResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3WriteResult write(Nfs3FileHandle handle, long position, byte[] buffer, int bufferOffset, int count) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Write);
        handle.write(writer);
        writer.write(position);
        writer.write(count);
        writer.write(Nfs3StableHow.Unstable.ordinal());
        writer.writeBuffer(buffer, bufferOffset, count);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3WriteResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3CreateResult create(Nfs3FileHandle dirHandle, String name, boolean createNew, Nfs3SetAttributes attributes) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Create);
        dirHandle.write(writer);
        writer.write(name);
        writer.write(createNew ? 1 : 0);
        attributes.write(writer);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3CreateResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3CreateResult makeDirectory(Nfs3FileHandle dirHandle, String name, Nfs3SetAttributes attributes) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Mkdir);
        dirHandle.write(writer);
        writer.write(name);
        attributes.write(writer);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3CreateResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3ModifyResult remove(Nfs3FileHandle dirHandle, String name) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Remove);
        dirHandle.write(writer);
        writer.write(name);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3ModifyResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3ModifyResult removeDirectory(Nfs3FileHandle dirHandle, String name) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Rmdir);
        dirHandle.write(writer);
        writer.write(name);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3ModifyResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3RenameResult rename(Nfs3FileHandle fromDirHandle, String fromName, Nfs3FileHandle toDirHandle, String toName) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Rename);
        fromDirHandle.write(writer);
        writer.write(fromName);
        toDirHandle.write(writer);
        writer.write(toName);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3RenameResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3ReadDirPlusResult readDirPlus(Nfs3FileHandle dir, long cookie, long cookieVerifier, int dirCount, int maxCount) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.ReadDirPlus);
        dir.write(writer);
        writer.write(cookie);
        writer.write(cookieVerifier);
        writer.write(dirCount);
        writer.write(maxCount);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            return new Nfs3ReadDirPlusResult(reply.getBodyReader());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3FileSystemInfoResult fileSystemInfo(Nfs3FileHandle fileHandle) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Fsinfo);
        fileHandle.write(writer);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            Nfs3FileSystemInfoResult fsiReply = new Nfs3FileSystemInfoResult(reply.getBodyReader());
            if (fsiReply.getStatus() == Nfs3Status.Ok) {
                return fsiReply;
            }

            throw new Nfs3Exception(fsiReply.getStatus());
        }

        throw new RpcException(reply.getHeader().getReplyHeader());
    }

    public Nfs3FileSystemStatResult fileSystemStat(Nfs3FileHandle fileHandle) {
        MemoryStream ms = new MemoryStream();
        XdrDataWriter writer = startCallMessage(ms, _client.getCredentials(), NfsProc3.Fsstat);
        fileHandle.write(writer);
        RpcReply reply = doSend(ms);
        if (reply.getHeader().isSuccess()) {
            Nfs3FileSystemStatResult statReply = new Nfs3FileSystemStatResult(reply.getBodyReader());
            if (statReply.getStatus() == Nfs3Status.Ok) {
                return statReply;
            } else {
                throw new Nfs3Exception(statReply.getStatus());
            }
        } else {
            throw new RpcException(reply.getHeader().getReplyHeader());
        }
    }
}
