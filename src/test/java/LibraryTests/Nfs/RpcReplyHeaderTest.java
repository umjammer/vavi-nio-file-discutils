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

package LibraryTests.Nfs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Nfs.RpcAcceptStatus;
import DiscUtils.Nfs.RpcAcceptedReplyHeader;
import DiscUtils.Nfs.RpcAuthentication;
import DiscUtils.Nfs.RpcReplyHeader;
import DiscUtils.Nfs.RpcUnixCredential;
import DiscUtils.Nfs.XdrDataReader;
import DiscUtils.Nfs.XdrDataWriter;
import moe.yo3explorer.dotnetio4j.MemoryStream;


public class RpcReplyHeaderTest {
    @Test
    public void roundTripTest() throws Exception {
        RpcReplyHeader header = new RpcReplyHeader();
        RpcAcceptedReplyHeader accepted = new RpcAcceptedReplyHeader();
        accepted.AcceptStatus = RpcAcceptStatus.Success;
        accepted.MismatchInfo = null;
        accepted.Verifier = new RpcAuthentication(new RpcUnixCredential(1, 2));
        header.AcceptReply = accepted;

        RpcReplyHeader clone = null;

        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            header.write(writer);

            stream.setPosition(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new RpcReplyHeader(reader);
        }

        assertEquals(header, clone);
    }
}
