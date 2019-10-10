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

package LibraryTests.Iso9660;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Iso9660.BootDeviceEmulation;
import DiscUtils.Iso9660.CDBuilder;
import DiscUtils.Iso9660.CDReader;
import moe.yo3explorer.dotnetio4j.MemoryStream;
import moe.yo3explorer.dotnetio4j.Stream;

public class BuilderTest {
    public void addFileStream() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("ADIR\\AFILE.TXT", new MemoryStream());
        CDReader fs = new CDReader(builder.build(), false);
        assertTrue(fs.exists("ADIR\\AFILE.TXT"));
    }

    public void addFileBytes() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("ADIR\\AFILE.TXT", new byte[] {});
        CDReader fs = new CDReader(builder.build(), false);
        assertTrue(fs.exists("ADIR\\AFILE.TXT"));
    }

    public void bootImage() throws Exception {
        byte[] memoryStream = new byte[33 * 512];
        for (int i = 0; i < memoryStream.length; ++i) {
            memoryStream[i] = (byte) i;
        }
        CDBuilder builder = new CDBuilder();
        builder.setBootImage(new MemoryStream(memoryStream), BootDeviceEmulation.HardDisk, 0x543);
        CDReader fs = new CDReader(builder.build(), false);
        assertTrue(fs.hasBootImage());
        Stream bootImg = fs.openBootImage();
        try {
            {
                assertEquals(memoryStream.length, bootImg.getLength());
                for (int i = 0; i < bootImg.getLength(); ++i) {
                    if (memoryStream[i] != bootImg.readByte()) {
                        assertTrue(false, "Boot image corrupted");
                    }

                }
            }
        } finally {
            if (bootImg != null)
                bootImg.close();

        }
        assertEquals(BootDeviceEmulation.HardDisk, fs.getBootEmulation());
        assertEquals(0x543, fs.getBootLoadSegment());
    }
}
