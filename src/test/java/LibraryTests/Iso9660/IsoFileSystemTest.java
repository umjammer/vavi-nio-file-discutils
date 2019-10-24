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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.DiscDirectoryInfo;
import DiscUtils.Core.DiscFileInfo;
import DiscUtils.Core.DiscFileSystemInfo;
import DiscUtils.Iso9660.CDBuilder;
import DiscUtils.Iso9660.CDReader;
import DiscUtils.Streams.SparseStream;
import dotnet4j.io.MemoryStream;

public class IsoFileSystemTest {
    @Test
    public void canWrite() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        assertFalse(fs.canWrite());
    }

    @Test
    public void fileInfo() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        DiscFileInfo fi = fs.getFileInfo("SOMEDIR\\SOMEFILE.TXT");
        assertNotNull(fi);
    }

    @Test
    public void directoryInfo() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        DiscDirectoryInfo fi = fs.getDirectoryInfo("SOMEDIR");
        assertNotNull(fi);
    }

    @Test
    public void fileSystemInfo() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        DiscFileSystemInfo fi = fs.getFileSystemInfo("SOMEDIR\\SOMEFILE");
        assertNotNull(fi);
    }

    @Test
    public void root() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        assertNotNull(fs.getRoot());
        assertTrue(fs.getRoot().exists());
        assertTrue(fs.getRoot().getName().isEmpty());
        assertNull(fs.getRoot().getParent());
    }

    @Test
    public void largeDirectory() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.setUseJoliet(true);
        for (int i = 0; i < 3000; ++i) {
            builder.addFile("FILE" + i + ".TXT", new byte[] {});
        }
        CDReader reader = new CDReader(builder.build(), true);
        assertEquals(3000, reader.getRoot().getFiles().size());
    }

    @Test
    public void hideVersions() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.setUseJoliet( true);
        builder.addFile("FILE.TXT;1", new byte[] {});
        MemoryStream ms = new MemoryStream();
        SparseStream.pump(builder.build(), ms);
        CDReader reader = new CDReader(ms, true, false);
        assertEquals("\\FILE.TXT;1", reader.getFiles("").get(0));
        assertEquals("\\FILE.TXT;1", reader.getFileSystemEntries("").get(0));
        reader = new CDReader(ms, true, true);
        assertEquals("\\FILE.TXT", reader.getFiles("").get(0));
        assertEquals("\\FILE.TXT", reader.getFileSystemEntries("").get(0));
    }
}
