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


public class RpcMessageHeader {
    public RpcMessageHeader() {
    }

    public RpcMessageHeader(XdrDataReader reader) {
        setTransactionId(reader.readUInt32());
        RpcMessageType type = RpcMessageType.values()[reader.readInt32()];
        if (type != RpcMessageType.Reply) {
            throw new UnsupportedOperationException("Parsing RPC call messages");
        }

        setReplyHeader(new RpcReplyHeader(reader));
    }

    public boolean isSuccess() {
        return replyHeader != null && replyHeader.Status == RpcReplyStatus.Accepted
                && replyHeader.acceptReply.AcceptStatus == RpcAcceptStatus.Success;
    }

    private RpcReplyHeader replyHeader;

    public RpcReplyHeader getReplyHeader() {
        return replyHeader;
    }

    public void setReplyHeader(RpcReplyHeader value) {
        replyHeader = value;
    }

    private int transactionId;

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int value) {
        transactionId = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(transactionId);
        writer.write(RpcMessageType.Reply.ordinal());
        replyHeader.write(writer);
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof RpcMessageHeader ? (RpcMessageHeader) obj : null);
    }

    public boolean equals(RpcMessageHeader other) {
        if (other == null) {
            return false;
        }

        return other.isSuccess() == isSuccess() && other.transactionId == transactionId
                && other.replyHeader.equals(replyHeader);
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(isSuccess(), transactionId, replyHeader);
    }

    public static RpcMessageHeader accepted(int transactionId) {
        RpcMessageHeader header = new RpcMessageHeader();
        header.setTransactionId(transactionId);
        RpcReplyHeader replyHeader = new RpcReplyHeader();
        replyHeader.Status = RpcReplyStatus.Accepted;
        RpcAcceptedReplyHeader acceptedReplyHeader = new RpcAcceptedReplyHeader();
        acceptedReplyHeader.AcceptStatus = RpcAcceptStatus.Success;
        acceptedReplyHeader.verifier = RpcAuthentication.null_();
        replyHeader.acceptReply = acceptedReplyHeader;
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
        acceptedReplyHeader.mismatchInfo = new RpcMismatchInfo();
        acceptedReplyHeader.verifier = RpcAuthentication.null_();
        replyHeader.acceptReply = acceptedReplyHeader;
        header.setReplyHeader(replyHeader);
        return header;
    }
}
