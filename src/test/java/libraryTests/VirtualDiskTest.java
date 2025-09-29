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

package libraryTests;

import discUtils.streams.util.Ownership;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


public class VirtualDiskTest {

    @Test
    public void testSignature() throws Exception {
        MemoryStream ms = new MemoryStream();
        ms.setLength(1024 * 1024);
        discUtils.core.raw.Disk rawDisk = new discUtils.core.raw.Disk(ms, Ownership.Dispose);
        assertEquals(0, rawDisk.getSignature());
        rawDisk.setSignature(0xDEADBEEF);
        assertEquals(0xDEADBEEF, rawDisk.getSignature());
    }

    @Test
    public void testMbr() throws Exception {
        MemoryStream ms = new MemoryStream();
        ms.setLength(1024 * 1024);
        byte[] newMbr = new byte[512];
        for (int i = 0; i < 512; i++) {
            newMbr[i] = (byte) i;
        }
        discUtils.core.raw.Disk rawDisk = new discUtils.core.raw.Disk(ms, Ownership.Dispose);
        rawDisk.setMasterBootRecord(newMbr);
        byte[] readMbr = rawDisk.getMasterBootRecord();
        assertEquals(512, readMbr.length);
        for (int i = 0; i < 512; i++) {
            if (readMbr[i] != (byte) i) {
                fail("Mismatch on byte %d, expected %2x was %2x".formatted(i, (byte) i, readMbr[i]));
            }
        }
    }

    @Test
    public void testMbr_Null() throws Exception {
        MemoryStream ms = new MemoryStream();
        ms.setLength(1024 * 1024);
        discUtils.core.raw.Disk rawDisk = new discUtils.core.raw.Disk(ms, Ownership.Dispose);
        assertThrows(NullPointerException.class, () -> rawDisk.setMasterBootRecord(null));
    }

    @Test
    public void testMbr_WrongSize() throws Exception {
        MemoryStream ms = new MemoryStream();
        ms.setLength(1024 * 1024);
        discUtils.core.raw.Disk rawDisk = new discUtils.core.raw.Disk(ms, Ownership.Dispose);
        assertThrows(IllegalArgumentException.class, () -> rawDisk.setMasterBootRecord(new byte[511]));
    }
}
