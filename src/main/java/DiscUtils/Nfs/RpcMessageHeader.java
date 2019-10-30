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


public class RpcMessageHeader {
    public RpcMessageHeader() {
    }

    public RpcMessageHeader(XdrDataReader reader) {
        setTransactionId(reader.readUInt32());
        RpcMessageType type = RpcMessageType.valueOf(reader.readInt32());
        if (type != RpcMessageType.Reply) {
            throw new UnsupportedOperationException("Parsing RPC call messages");
        }

        setReplyHeader(new RpcReplyHeader(reader));
    }

    public boolean isSuccess() {
        return getReplyHeader() != null && getReplyHeader().Status == RpcReplyStatus.Accepted
                && getReplyHeader().AcceptReply.AcceptStatus == RpcAcceptStatus.Success;
    }

    private RpcReplyHeader __ReplyHeader;

    public RpcReplyHeader getReplyHeader() {
        return __ReplyHeader;
    }

    public void setReplyHeader(RpcReplyHeader value) {
        __ReplyHeader = value;
    }

    private int __TransactionId;

    public int getTransactionId() {
        return __TransactionId;
    }

    public void setTransactionId(int value) {
        __TransactionId = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getTransactionId());
        writer.write(RpcMessageType.Reply.ordinal());
        getReplyHeader().write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof RpcMessageHeader ? (RpcMessageHeader) obj : (RpcMessageHeader) null);
    }

    public boolean equals(RpcMessageHeader other) {
        if (other == null) {
            return false;
        }

        return other.isSuccess() == isSuccess() && other.getTransactionId() == getTransactionId()
                && other.getReplyHeader().equals(getReplyHeader());
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(isSuccess(), getTransactionId(), getReplyHeader());
    }

    public static RpcMessageHeader accepted(int transactionId) {
        RpcMessageHeader header = new RpcMessageHeader();
        header.setTransactionId(transactionId);
        RpcReplyHeader replyHeader = new RpcReplyHeader();
        replyHeader.Status = RpcReplyStatus.Accepted;
        RpcAcceptedReplyHeader acceptedReplyHeader = new RpcAcceptedReplyHeader();
        acceptedReplyHeader.AcceptStatus = RpcAcceptStatus.Success;
        acceptedReplyHeader.Verifier = RpcAuthentication.null_();
        replyHeader.AcceptReply = acceptedReplyHeader;
        header.setReplyHeader(replyHeader);
        return header;
    }

    public static RpcMessageHeader procedureUnavailable(int transactionId) {
        RpcMessageHeader header = new RpcMessageHeader();
        header.setTransactionId(transactionId);
        RpcReplyHeader replyHeader = new RpcReplyHeader();
        replyHeader.Status = RpcReplyStatus.Accepted;
        RpcAcceptedReplyHeader acceptedReplyHeader = new RpcAcceptedReplyHeader();
        acceptedReplyHeader.AcceptStatus = RpcAcceptStatus.ProcedureUnavailable;
        acceptedReplyHeader.MismatchInfo = new RpcMismatchInfo();
        acceptedReplyHeader.Verifier = RpcAuthentication.null_();
        replyHeader.AcceptReply = acceptedReplyHeader;
        header.setReplyHeader(replyHeader);
        return header;
    }
}
