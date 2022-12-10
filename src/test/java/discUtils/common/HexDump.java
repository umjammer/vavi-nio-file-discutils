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

package discUtils.common;

import java.io.PrintStream;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Range;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;


/**
 * Provides utility methods to produce hex dumps of binary data.
 */
public class HexDump {
    /**
     * Creates a hex dump from a byte array.
     *
     * @param data The buffer to generate the hex dump from.
     * @param output The destination for the hex dump.
     */
    public static void generate(byte[] data, PrintStream output) {
        generate(SparseStream.fromStream(new MemoryStream(data, false), Ownership.None), output);
    }

    /**
     * Creates a hex dump from a byte array.
     *
     * @param data The buffer to generate the hex dump from.
     * @param offset Offset of the first byte to hex dump.
     * @param count The number of bytes to hex dump
     * @param output The destination for the hex dump.
     */
    public static void generate(byte[] data, int offset, int count, PrintStream output) {
        byte[] tempBuffer = new byte[count];
        System.arraycopy(data, offset, tempBuffer, 0, count);
        generate(SparseStream.fromStream(new MemoryStream(tempBuffer, false), Ownership.None), output);
    }

    /**
     * Creates a hex dump from a stream.
     *
     * @param stream The stream to generate the hex dump from.
     * @param output The destination for the hex dump.
     */
    public static void generate(Stream stream, PrintStream output) {
        generate(SparseStream.fromStream(stream, Ownership.None), output);
    }

    /**
     * Creates a hex dump from a stream.
     *
     * @param stream The stream to generate the hex dump from.
     * @param output The destination for the hex dump.
     */
    public static void generate(SparseStream stream, PrintStream output) {
        stream.position(0);
        byte[] buffer = new byte[1024 * 1024];
        for (Range block : StreamExtent.blocks(stream.getExtents(), buffer.length)) {
            long startPos = block.getOffset() * buffer.length;
            long endPos = Math.min((block.getOffset() + block.getCount()) * buffer.length, stream.getLength());
            stream.position(startPos);
            while (stream.position() < endPos) {
                int numLoaded = 0;
                long readStart = stream.position();
                while (numLoaded < buffer.length) {
                    int bytesRead = stream.read(buffer, numLoaded, buffer.length - numLoaded);
                    if (bytesRead == 0) {
                        break;
                    }

                    numLoaded += bytesRead;
                }
                for (int i = 0; i < numLoaded; i += 16) {
                    boolean foundVal = false;
                    if (i > 0) {
                        for (int j = 0; j < 16; j++) {
                            if (buffer[i + j] != buffer[i + j - 16]) {
                                foundVal = true;
                                break;
                            }

                        }
                    } else {
                        foundVal = true;
                    }
                    if (foundVal) {
                        output.printf("%08x", i + readStart);
                        for (int j = 0; j < 16; j++) {
                            if (j % 8 == 0) {
                                output.print(" ");
                            }

                            output.printf(" %02x", buffer[i + j]);
                        }
                        output.print("  |");
                        for (int j = 0; j < 16; j++) {
                            if (j % 8 == 0 && j != 0) {
                                output.print(" ");
                            }

                            output.printf("%c", (buffer[i + j] >= 32 && buffer[i + j] < 127) ? (char) buffer[i + j] : '.');
                        }
                        output.print("|");
                        output.println();
                    }
                }
            }
        }
    }
}
