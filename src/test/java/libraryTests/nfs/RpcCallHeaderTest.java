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

import discUtils.nfs.NfsProc3;
import discUtils.nfs.RpcAuthentication;
import discUtils.nfs.RpcCallHeader;
import discUtils.nfs.XdrDataReader;
import discUtils.nfs.XdrDataWriter;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class RpcCallHeaderTest {

    @Test
    void roundTripTest() throws Exception {
        RpcCallHeader header = new RpcCallHeader();
        header.setCredentials(new RpcAuthentication());
        header.setProc(NfsProc3.Commit.ordinal());
        header.setProgram(5);
        header.setRpcVersion(6);
        header.setVerifier(new RpcAuthentication());
        header.setVersion(7);
        RpcCallHeader clone;

        try (MemoryStream stream = new MemoryStream()) {
            XdrDataWriter writer = new XdrDataWriter(stream);
            header.write(writer);
            stream.position(0);
            XdrDataReader reader = new XdrDataReader(stream);
            clone = new RpcCallHeader(reader);
        }
        assertEquals(header, clone);
    }
}
