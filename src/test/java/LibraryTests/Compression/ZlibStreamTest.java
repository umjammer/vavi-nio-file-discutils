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

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import DiscUtils.Core.Compression.ZlibStream;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.compression.CompressionMode;


public class ZlibStreamTest {
    @Test
    public void testRoundtrip() throws Exception {
        byte[] testData = "This is a test string".getBytes(Charset.forName("ASCII"));

        MemoryStream compressedStream = new MemoryStream();

        try (ZlibStream zs = new ZlibStream(compressedStream, CompressionMode.Compress, true)) {
            zs.write(testData, 0, testData.length);
        }

//Debug.println("\n" + StringUtil.getDump(compressedStream.toArray()));
        compressedStream.setPosition(0);
        try (ZlibStream uzs = new ZlibStream(compressedStream, CompressionMode.Decompress, true)) {
            byte[] outData = new byte[testData.length];
            uzs.read(outData, 0, outData.length);
            assertArrayEquals(testData, outData);

            // Should be end of stream
            assertEquals(-1, uzs.readByte());
        }
    }

    @Test
    public void testInvalidChecksum() throws Exception {
        byte[] testData = "This is a test string".getBytes(Charset.forName("ASCII"));

        MemoryStream compressedStream = new MemoryStream();

        try (ZlibStream zs = new ZlibStream(compressedStream, CompressionMode.Compress, true)) {
            zs.write(testData, 0, testData.length);
        }

        compressedStream.seek(-2, SeekOrigin.End);
        compressedStream.write(new byte[] {
            0, 0
        }, 0, 2);

        compressedStream.setPosition(0);
        assertThrows(IOException.class, () -> {
            try (ZlibStream uzs = new ZlibStream(compressedStream, CompressionMode.Decompress, true)) {
                byte[] outData = new byte[testData.length];
                uzs.read(outData, 0, outData.length);
                assertArrayEquals(testData, outData);
                // Should be end of stream
                assertEquals(-1, uzs.readByte());
            }
        });
    }
}
