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


public class RpcAcceptedReplyHeader {
    public RpcAcceptStatus AcceptStatus = RpcAcceptStatus.Success;

    public RpcMismatchInfo MismatchInfo;

    public RpcAuthentication Verifier;

    public RpcAcceptedReplyHeader() {
    }

    public RpcAcceptedReplyHeader(XdrDataReader reader) {
        Verifier = new RpcAuthentication(reader);
        AcceptStatus = RpcAcceptStatus.values()[reader.readInt32()];
        if (AcceptStatus == RpcAcceptStatus.ProgramVersionMismatch) {
            MismatchInfo = new RpcMismatchInfo(reader);
        }

    }

    public void write(XdrDataWriter writer) {
        Verifier.write(writer);
        writer.write(AcceptStatus.ordinal());
        if (AcceptStatus == RpcAcceptStatus.ProgramVersionMismatch) {
            MismatchInfo.write(writer);
        }

    }

    public boolean equals(Object obj) {
        return equals(obj instanceof RpcAcceptedReplyHeader ? (RpcAcceptedReplyHeader) obj : (RpcAcceptedReplyHeader) null);
    }

    public boolean equals(RpcAcceptedReplyHeader other) {
        if (other == null) {
            return false;
        }

        return dotnet4j.io.compat.Utilities.equals(other.Verifier, Verifier) && other.AcceptStatus == AcceptStatus
                && dotnet4j.io.compat.Utilities.equals(other.MismatchInfo, MismatchInfo);
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(Verifier, AcceptStatus, MismatchInfo);
    }
}
