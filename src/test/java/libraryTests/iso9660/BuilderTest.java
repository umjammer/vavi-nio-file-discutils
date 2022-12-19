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

package libraryTests.iso9660;

import java.io.File;

import discUtils.iso9660.BootDeviceEmulation;
import discUtils.iso9660.CDBuilder;
import discUtils.iso9660.CDReader;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class BuilderTest {

    private static final String FS = File.separator;

    @Test
    public void addFileStream() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("ADIR" + FS + "AFILE.TXT", new MemoryStream());
        CDReader fs = new CDReader(builder.build(), false);
        assertTrue(fs.exists("ADIR" + FS + "AFILE.TXT"));
    }

    @Test
    public void addFileBytes() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("ADIR" + FS + "AFILE.TXT", new byte[] {});
        CDReader fs = new CDReader(builder.build(), false);
        assertTrue(fs.exists("ADIR" + FS + "AFILE.TXT"));
    }

    @Test
    public void bootImage() throws Exception {
        byte[] memoryStream = new byte[33 * 512];
        for (int i = 0; i < memoryStream.length; ++i) {
            memoryStream[i] = (byte) i;
        }
        CDBuilder builder = new CDBuilder();
        builder.setBootImage(new MemoryStream(memoryStream), BootDeviceEmulation.HardDisk, 0x543);
        CDReader fs = new CDReader(builder.build(), false);
        assertTrue(fs.hasBootImage());

        try (Stream bootImg = fs.openBootImage()) {
            assertEquals(memoryStream.length, bootImg.getLength());
            for (int i = 0; i < bootImg.getLength(); ++i) {
                if ((memoryStream[i] & 0xff) != bootImg.readByte()) {
                    fail("Boot image corrupted");
                }
            }
        }
        assertEquals(BootDeviceEmulation.HardDisk, fs.getBootEmulation());
        assertEquals(0x543, fs.getBootLoadSegment());
    }
}
