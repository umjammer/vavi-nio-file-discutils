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

package libraryTests.squashFs;

import discUtils.squashFs.SquashFileSystemBuilder;
import discUtils.squashFs.SquashFileSystemReader;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public final class SquashFileSystemReaderTest {
    @Test
    public void detect() {
        MemoryStream ms = new MemoryStream(new byte[1000]);
        assertFalse(SquashFileSystemReader.detect(ms));
        ms = new MemoryStream(new byte[10]);
        assertFalse(SquashFileSystemReader.detect(ms));
        MemoryStream emptyFs = new MemoryStream();
        SquashFileSystemBuilder builder = new SquashFileSystemBuilder();
        builder.build(emptyFs);
        assertTrue(SquashFileSystemReader.detect(emptyFs));
    }
}
