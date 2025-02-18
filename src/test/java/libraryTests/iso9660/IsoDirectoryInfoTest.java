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
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.iso9660.CDBuilder;
import discUtils.iso9660.CDReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IsoDirectoryInfoTest {

    private static final String FS = File.separator;

    @Test
    public void exists() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("SOMEDIR" + FS + "CHILDDIR" + FS + "FILE.TXT", new byte[0]);
        CDReader fs = new CDReader(builder.build(), false);
        assertTrue(fs.getDirectoryInfo(FS).exists());
        assertTrue(fs.getDirectoryInfo("SOMEDIR").exists());
        assertTrue(fs.getDirectoryInfo("SOMEDIR" + FS + "CHILDDIR").exists());
        assertTrue(fs.getDirectoryInfo("SOMEDIR" + FS + "CHILDDIR" + FS).exists());
        assertFalse(fs.getDirectoryInfo("NONDIR").exists());
        assertFalse(fs.getDirectoryInfo("SOMEDIR" + FS + "NONDIR").exists());
    }

    @Test
    public void fullName() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        assertEquals(FS, fs.getRoot().getFullName());
        assertEquals("SOMEDIR" + FS, fs.getDirectoryInfo("SOMEDIR").getFullName());
        assertEquals("SOMEDIR" + FS + "CHILDDIR" + FS, fs.getDirectoryInfo("SOMEDIR" + FS + "CHILDDIR").getFullName());
    }

    @Test
    public void simpleSearch() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addFile("SOMEDIR" + FS + "CHILDDIR" + FS + "GCHILDIR" + FS + "FILE.TXT", new byte[0]);
        CDReader fs = new CDReader(builder.build(), false);
        DiscDirectoryInfo di = fs.getDirectoryInfo("SOMEDIR" + FS + "CHILDDIR");
        List<DiscFileInfo> fis = di.getFiles("*.*", "AllDirectories");
    }

    @Test
    public void extension() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        assertEquals("dir", fs.getDirectoryInfo("fred.dir").getExtension());
        assertEquals("", fs.getDirectoryInfo("fred").getExtension());
    }

    @Test
    public void getDirectories() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addDirectory("SOMEDIR" + FS + "CHILD" + FS + "GCHILD");
        builder.addDirectory("A.DIR");
        CDReader fs = new CDReader(builder.build(), false);
        assertEquals(2, fs.getRoot().getDirectories().size());
        DiscDirectoryInfo someDir = fs.getRoot().getDirectories("SoMeDir").get(0);
        assertEquals(1, fs.getRoot().getDirectories("SOMEDIR").size());
        assertEquals("SOMEDIR", someDir.getName());
        assertEquals(1, someDir.getDirectories("*.*").size());
        assertEquals("CHILD", someDir.getDirectories("*.*").get(0).getName());
        assertEquals(2, someDir.getDirectories("*.*", "AllDirectories").size());
        assertEquals(4, fs.getRoot().getDirectories("*.*", "AllDirectories").size());
        assertEquals(2, fs.getRoot().getDirectories("*.*", "TopDirectoryOnly").size());
        assertEquals(1, fs.getRoot().getDirectories("*.DIR", "AllDirectories").size());
        assertEquals("A.DIR" + FS, fs.getRoot().getDirectories("*.DIR", "AllDirectories").get(0).getFullName());
        assertEquals(1, fs.getRoot().getDirectories("GCHILD", "AllDirectories").size());
        assertEquals("SOMEDIR" + FS + "CHILD" + FS + "GCHILD" + FS, fs.getRoot().getDirectories("GCHILD", "AllDirectories").get(0).getFullName());
    }

    @Test
    public void getFiles() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addDirectory("SOMEDIR" + FS + "CHILD" + FS + "GCHILD");
        builder.addDirectory("AAA.DIR");
        builder.addFile("FOO.TXT", new byte[10]);
        builder.addFile("SOMEDIR" + FS + "CHILD.TXT", new byte[10]);
        builder.addFile("SOMEDIR" + FS + "FOO.TXT", new byte[10]);
        builder.addFile("SOMEDIR" + FS + "CHILD" + FS + "GCHILD" + FS + "BAR.TXT", new byte[10]);
        CDReader fs = new CDReader(builder.build(), false);
        assertEquals(1, fs.getRoot().getFiles().size());
        assertEquals("FOO.TXT", fs.getRoot().getFiles().get(0).getFullName());
        assertEquals(2, fs.getRoot().getDirectories("SOMEDIR").get(0).getFiles("*.TXT").size());
        assertEquals(4, fs.getRoot().getFiles("*.TXT", "AllDirectories").size());
        assertEquals(0, fs.getRoot().getFiles("*.DIR", "AllDirectories").size());
    }

    @Test
    public void getFileSystemInfos() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addDirectory("SOMEDIR" + FS + "CHILD" + FS + "GCHILD");
        builder.addDirectory("AAA.EXT");
        builder.addFile("FOO.TXT", new byte[10]);
        builder.addFile("SOMEDIR" + FS + "CHILD.TXT", new byte[10]);
        builder.addFile("SOMEDIR" + FS + "FOO.TXT", new byte[10]);
        builder.addFile("SOMEDIR" + FS + "CHILD" + FS + "GCHILD" + FS + "BAR.TXT", new byte[10]);
        CDReader fs = new CDReader(builder.build(), false);
        assertEquals(3, fs.getRoot().getFileSystemInfos().size());
        assertEquals(1, fs.getRoot().getFileSystemInfos("*.EXT").size());
        assertEquals(2, fs.getRoot().getFileSystemInfos("*.?XT").size());
    }

    @Test
    public void parent() throws Exception {
        CDBuilder builder = new CDBuilder();
        builder.addDirectory("SOMEDIR");
        CDReader fs = new CDReader(builder.build(), false);
        assertEquals(fs.getRoot(), fs.getRoot().getDirectories("SOMEDIR").get(0).getParent());
    }

    @Test
    public void parent_Root() throws Exception {
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        assertNull(fs.getRoot().getParent());
    }

    @Test
    public void rootBehaviour() throws Exception {
        // Start time rounded down to whole seconds
        Instant start_ = Instant.now();
//        start = LocalDateTime.of(start.getYear, start.Month, start.Day, start.Hour, start.Minute, start.Second);
        long start = start_.minusNanos(start_.getNano()).toEpochMilli();
        CDBuilder builder = new CDBuilder();
        CDReader fs = new CDReader(builder.build(), false);
        long end = Instant.now().toEpochMilli();
        assertEquals(EnumSet.of(FileAttributes.Directory, FileAttributes.ReadOnly), fs.getRoot().getAttributes());
        assertTrue(fs.getRoot().getCreationTimeUtc() >= start);
        assertTrue(fs.getRoot().getCreationTimeUtc() <= end);
        assertTrue(fs.getRoot().getLastAccessTimeUtc() >= start);
        assertTrue(fs.getRoot().getLastAccessTimeUtc() <= end);
        assertTrue(fs.getRoot().getLastWriteTimeUtc() >= start);
        assertTrue(fs.getRoot().getLastWriteTimeUtc() <= end);
    }

    @Test
    public void attributes() throws Exception {
        // Start time rounded down to whole seconds
        Instant start_ = Instant.now();
//        start = LocalDateTime.of(start.Year, start.Month, start.Day, start.Hour, start.Minute, start.Second);
        long start = start_.minusNanos(start_.getNano()).toEpochMilli();
        CDBuilder builder = new CDBuilder();
        builder.addDirectory("Foo");
        CDReader fs = new CDReader(builder.build(), false);
        long end = Instant.now().toEpochMilli();
        DiscDirectoryInfo di = fs.getDirectoryInfo("Foo");
        assertEquals(EnumSet.of(FileAttributes.Directory, FileAttributes.ReadOnly), di.getAttributes());
        assertTrue(di.getCreationTimeUtc() >= start);
        assertTrue(di.getCreationTimeUtc() <= end);
        assertTrue(di.getLastAccessTimeUtc() >= start);
        assertTrue(di.getLastAccessTimeUtc() <= end);
        assertTrue(di.getLastWriteTimeUtc() >= start);
        assertTrue(di.getLastWriteTimeUtc() <= end);
    }
}
