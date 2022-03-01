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

package LibraryTests.Compression;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import DiscUtils.Core.Compression.BZip2DecoderStream;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;


public class BZip2DecoderStreamTest {
    private final byte[] ValidData = {
        0x42, 0x5A, 0x68, 0x39, 0x31, 0x41, 0x59, 0x26, 0x53, 0x59, (byte) 0xF7, 0x3C, 0x46, 0x3E, 0x00, 0x00, 0x02, 0x13,
        (byte) 0x80, 0x40, 0x00, 0x04, 0x00, 0x22, (byte) 0xE1, 0x1C, 0x00, 0x20, 0x00, 0x31, 0x00, (byte) 0xD3, 0x4D, 0x04,
        0x4F, 0x53, 0x6A, 0x7A, 0x4F, 0x23, (byte) 0xA8, 0x51, (byte) 0xB6, (byte) 0xA1, 0x71, 0x0A, (byte) 0x86, 0x01,
        (byte) 0xA2, (byte) 0xEE, 0x48, (byte) 0xA7, 0x0A, 0x12, 0x1E, (byte) 0xE7, (byte) 0x88, (byte) 0xC7, (byte) 0xC0
    };

    private final byte[] InvalidBlockCrcData = {
        0x42, 0x5A, 0x68, 0x39, 0x31, 0x41, 0x59, 0x26, 0x53, 0x59, (byte) 0xFF, 0x3C, 0x46, 0x3E, 0x00, 0x00, 0x02, 0x13,
        (byte) 0x80, 0x40, 0x00, 0x04, 0x00, 0x22, (byte) 0xE1, 0x1C, 0x00, 0x20, 0x00, 0x31, 0x00, (byte) 0xD3, 0x4D, 0x04,
        0x4F, 0x53, 0x6A, 0x7A, 0x4F, 0x23, (byte) 0xA8, 0x51, (byte) 0xB6, (byte) 0xA1, 0x71, 0x0A, (byte) 0x86, 0x01,
        (byte) 0xA2, (byte) 0xEE, 0x48, (byte) 0xA7, 0x0A, 0x12, 0x1E, (byte) 0xE7, (byte) 0x88, (byte) 0xC7, (byte) 0xC0
    };

    private final byte[] InvalidCombinedCrcData = {
        0x42, 0x5A, 0x68, 0x39, 0x31, 0x41, 0x59, 0x26, 0x53, 0x59, (byte) 0xF7, 0x3C, 0x46, 0x3E, 0x00, 0x00, 0x02, 0x13,
        (byte) 0x80, 0x40, 0x00, 0x04, 0x00, 0x22, (byte) 0xE1, 0x1C, 0x00, 0x20, 0x00, 0x31, 0x00, (byte) 0xD3, 0x4D, 0x04,
        0x4F, 0x53, 0x6A, 0x7A, 0x4F, 0x23, (byte) 0xA8, 0x51, (byte) 0xB6, (byte) 0xA1, 0x71, 0x0A, (byte) 0x86, 0x01,
        (byte) 0xA2, (byte) 0xEE, 0x48, (byte) 0xA7, 0x0A, 0x12, 0x1E, (byte) 0xE7, (byte) 0x88, (byte) 0xCF, (byte) 0xC0
    };

    @Test
    public void testValidStream() throws Exception {
        BZip2DecoderStream decoder = new BZip2DecoderStream(new MemoryStream(ValidData), Ownership.Dispose);
        byte[] buffer = new byte[1024];
        int numRead = decoder.read(buffer, 0, 1024);
        assertEquals(21, numRead);
        // Reading beyond the end of the stream will return 0 bytes
        assertEquals(0, decoder.read(buffer, numRead, 1024 - numRead));
        String s = new String(buffer, 0, numRead, StandardCharsets.US_ASCII);
        assertEquals("This is a test string", s);
    }

    @Test
    public void testInvalidBlockCrcStream() throws Exception {
        BZip2DecoderStream decoder = new BZip2DecoderStream(new MemoryStream(InvalidBlockCrcData), Ownership.Dispose);
        byte[] buffer = new byte[1024];
        assertThrows(IOException.class, () -> {
            decoder.read(buffer, 0, 1024);
        });
    }

    @Test
    public void testCombinedCrcStream() throws Exception {
        BZip2DecoderStream decoder = new BZip2DecoderStream(new MemoryStream(InvalidCombinedCrcData), Ownership.Dispose);
        byte[] buffer = new byte[1024];
        assertThrows(IOException.class, () -> {
            decoder.read(buffer, 0, 1024);
        });
    }

    @Test
    public void testCombinedCrcStream_ExactLengthRead() throws Exception {
        BZip2DecoderStream decoder = new BZip2DecoderStream(new MemoryStream(InvalidCombinedCrcData), Ownership.Dispose);
        byte[] buffer = new byte[21];
        assertThrows(IOException.class, () -> {
            decoder.read(buffer, 0, 21);
        });
    }
}
