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
import DiscUtils.Nfs.RpcMismatchInfo;
import DiscUtils.Nfs.XdrDataReader;
import DiscUtils.Nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;


public class RpcAcceptedReplyHeaderTest {
    @Test
    public void roundTripTest() throws Exception {
        RpcAcceptedReplyHeader header = new RpcAcceptedReplyHeader();
        header.AcceptStatus = RpcAcceptStatus.ProgramVersionMismatch;
        RpcMismatchInfo info = new RpcMismatchInfo();
        info.High = 1;
        info.Low = 2;
        header.MismatchInfo = info;

        header.Verifier = new RpcAuthentication();
        RpcAcceptedReplyHeader clone = null;
        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            header.write(writer);
            stream.setPosition(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new RpcAcceptedReplyHeader(reader);
        }
        assertEquals(header, clone);
    }
}
