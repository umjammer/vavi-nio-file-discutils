//
// Copyright (c) 2017, Glen Parker
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

package libraryTests.buffers;

import discUtils.streams.SparseMemoryBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class BufferTest {

    @Test
    void sparseMemoryBufferClear() throws Exception {
        SparseMemoryBuffer memoryBuffer = new SparseMemoryBuffer(10);
        byte[] buffer = new byte[20];
        memoryBuffer.write(0, buffer, 0, 20);
        assertEquals(2, memoryBuffer.getAllocatedChunks().size());
        memoryBuffer.clear(0, 20);
        assertEquals(0, memoryBuffer.getAllocatedChunks().size());
        memoryBuffer.write(0, buffer, 0, 15);
        assertEquals(2, memoryBuffer.getAllocatedChunks().size());
        memoryBuffer.clear(0, 15);
        assertEquals(1, memoryBuffer.getAllocatedChunks().size());
    }
}
