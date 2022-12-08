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

package libraryTests.nfs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import discUtils.nfs.RpcAcceptStatus;
import discUtils.nfs.RpcAcceptedReplyHeader;
import discUtils.nfs.RpcAuthentication;
import discUtils.nfs.RpcReplyHeader;
import discUtils.nfs.RpcUnixCredential;
import discUtils.nfs.XdrDataReader;
import discUtils.nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;


public class RpcReplyHeaderTest {
    @Test
    public void roundTripTest() throws Exception {
        RpcReplyHeader header = new RpcReplyHeader();
        RpcAcceptedReplyHeader accepted = new RpcAcceptedReplyHeader();
        accepted.AcceptStatus = RpcAcceptStatus.Success;
        accepted.mismatchInfo = null;
        accepted.verifier = new RpcAuthentication(new RpcUnixCredential(1, 2));
        header.acceptReply = accepted;

        RpcReplyHeader clone = null;

        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            header.write(writer);

            stream.position(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new RpcReplyHeader(reader);
        }

        assertEquals(header, clone);
    }
}
