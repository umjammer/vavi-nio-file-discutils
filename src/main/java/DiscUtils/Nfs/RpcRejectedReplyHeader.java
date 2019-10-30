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

import DiscUtils.Core.Internal.Utilities;

public class RpcRejectedReplyHeader {
    public RpcAuthenticationStatus AuthenticationStatus = RpcAuthenticationStatus.None;

    public RpcMismatchInfo MismatchInfo;

    public RpcRejectedStatus Status = RpcRejectedStatus.RpcMismatch;

    public RpcRejectedReplyHeader() {
    }

    public RpcRejectedReplyHeader(XdrDataReader reader) {
        Status = RpcRejectedStatus.valueOf(reader.readInt32());
        if (Status == RpcRejectedStatus.RpcMismatch) {
            MismatchInfo = new RpcMismatchInfo(reader);
        } else {
            AuthenticationStatus = RpcAuthenticationStatus.valueOf(reader.readInt32());
        }
    }

    public void write(XdrDataWriter writer) {
        writer.write(Status.ordinal());
        if (Status == RpcRejectedStatus.RpcMismatch) {
            MismatchInfo.write(writer);
        } else {
            writer.write(AuthenticationStatus.ordinal());
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof RpcRejectedReplyHeader ? (RpcRejectedReplyHeader) obj : (RpcRejectedReplyHeader) null);
    }

    public boolean equals(RpcRejectedReplyHeader other) {
        if (other == null) {
            return false;
        }

        return other.Status == Status && other.MismatchInfo.equals(MismatchInfo) &&
               other.AuthenticationStatus == AuthenticationStatus;
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(Status, MismatchInfo, AuthenticationStatus);
    }
}
