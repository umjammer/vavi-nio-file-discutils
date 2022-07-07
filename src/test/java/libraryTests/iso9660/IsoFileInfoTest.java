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

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import discUtils.core.DiscFileInfo;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.iso9660.CDBuilder;
import discUtils.iso9660.CDReader;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.Stream;


public class IsoFileInfoTest {
    @Test
    public void length() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("FILE.TXT", new byte[0]);
        builder.addFile("FILE2.TXT", new byte[1]);
        builder.addFile("FILE3.TXT", new byte[10032]);
        builder.addFile("FILE3.TXT;2", new byte[132]);
        CDReader fs = new CDReader(builder.build(), false);
        assertEquals(0, fs.getFileInfo("FILE.txt").getLength());
        assertEquals(1, fs.getFileInfo("FILE2.txt").getLength());
        assertEquals(10032, fs.getFileInfo("FILE3.txt;1").getLength());
        assertEquals(132, fs.getFileInfo("FILE3.txt;2").getLength());
        assertEquals(132, fs.getFileInfo("FILE3.txt").getLength());
    }

    @Test
    public void open_FileNotFound() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        DiscFileInfo di = fs.getFileInfo("foo.txt");
        assertThrows(FileNotFoundException.class, () -> {
            try (Stream s = di.open(FileMode.Open)) {
            }
        });
    }

    @Test
    public void open_Read() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("foo.txt",
                        new byte[] {
                            1
                        });
        CDReader fs = new CDReader(builder.build(), false);
        DiscFileInfo di = fs.getFileInfo("foo.txt");
        try (Stream s = di.open(FileMode.Open, FileAccess.Read)) {
            assertFalse(s.canWrite());
            assertTrue(s.canRead());
            assertEquals(1, s.readByte());
        }
    }

    @Test
    public void name() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        assertEquals("foo.txt", fs.getFileInfo("foo.txt").getName());
        assertEquals("foo.txt", fs.getFileInfo("path\\foo.txt").getName());
        assertEquals("foo.txt", fs.getFileInfo("\\foo.txt").getName());
    }

    @Test
    public void attributes() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("foo.txt",
                        new byte[] {
                            1
                        });
        CDReader fs = new CDReader(builder.build(), false);
        DiscFileInfo fi = fs.getFileInfo("foo.txt");
        // Check default attributes
        assertEquals(EnumSet.of(FileAttributes.ReadOnly), fi.getAttributes());
    }

    @Test
    public void exists() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("dir\\foo.txt",
                        new byte[] {
                            1
                        });
        CDReader fs = new CDReader(builder.build(), false);
        assertFalse(fs.getFileInfo("unknown.txt").exists());
        assertTrue(fs.getFileInfo("dir\\foo.txt").exists());
        assertFalse(fs.getFileInfo("dir").exists());
    }

    @Test
    public void creationTimeUtc() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("foo.txt",
                        new byte[] {
                            1
                        });
        CDReader fs = new CDReader(builder.build(), false);
        assertTrue(Instant.now().toEpochMilli() >= fs.getFileInfo("foo.txt").getCreationTimeUtc());
        assertTrue(Instant.now().minus(Duration.ofSeconds(10)).toEpochMilli() <= fs.getFileInfo("foo.txt")
                .getCreationTimeUtc());
    }

    @Test
    public void fileInfoEquals() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        assertEquals(fs.getFileInfo("foo.txt"), fs.getFileInfo("foo.txt"));
    }

    @Test
    public void parent() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("SOMEDIR\\ADIR\\FILE.TXT",
                        new byte[] {
                            1
                        });
        CDReader fs = new CDReader(builder.build(), false);
        DiscFileInfo fi = fs.getFileInfo("SOMEDIR\\ADIR\\FILE.TXT");
        assertEquals(fs.getDirectoryInfo("SOMEDIR\\ADIR"), fi.getParent());
        assertEquals(fs.getDirectoryInfo("SOMEDIR\\ADIR"), fi.getDirectory());
    }
}
