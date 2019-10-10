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

package LibraryTests;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.DiscDirectoryInfo;
import DiscUtils.Core.DiscFileSystem;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.Stream;


public class DiscFileSystemDirectoryTest {
    public void create(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscDirectoryInfo dirInfo = fs.getDirectoryInfo("SOMEDIR");
        dirInfo.create();
        assertEquals(1, fs.getRoot().getDirectories().size());
    }

    public void createRecursive(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscDirectoryInfo dirInfo = fs.getDirectoryInfo("SOMEDIR\\CHILDDIR");
        dirInfo.create();
        assertEquals(1, fs.getRoot().getDirectories().size());
        assertEquals(1, fs.getDirectoryInfo("SOMEDIR").getDirectories().size());
        assertEquals("CHILDDIR", fs.getDirectoryInfo("SOMEDIR").getDirectories().get(0).getName());
    }

    public void createExisting(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscDirectoryInfo dirInfo = fs.getDirectoryInfo("SOMEDIR");
        dirInfo.create();
        dirInfo.create();
        assertEquals(1, fs.getRoot().getDirectories().size());
    }

    public void createInvalid_Long(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscDirectoryInfo dirInfo = fs.getDirectoryInfo(new String(new char[256]).replace('\0', 'X'));
        assertThrows(IOException.class, () -> {
            dirInfo.create();
        });
    }

    public void createInvalid_Characters(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscDirectoryInfo dirInfo = fs.getDirectoryInfo("SOME\0DIR");
        assertThrows(IOException.class, () -> {
            dirInfo.create();
        });
    }

    public void exists(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscDirectoryInfo dirInfo = fs.getDirectoryInfo("SOMEDIR\\CHILDDIR");
        dirInfo.create();
        assertTrue(fs.getDirectoryInfo("\\").getExists());
        assertTrue(fs.getDirectoryInfo("SOMEDIR").getExists());
        assertTrue(fs.getDirectoryInfo("SOMEDIR\\CHILDDIR").getExists());
        assertTrue(fs.getDirectoryInfo("SOMEDIR\\CHILDDIR\\").getExists());
        assertFalse(fs.getDirectoryInfo("NONDIR").getExists());
        assertFalse(fs.getDirectoryInfo("SOMEDIR\\NONDIR").getExists());
    }

    public void fullName(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertEquals("\\", fs.getRoot().getFullName());
        assertEquals("SOMEDIR\\", fs.getDirectoryInfo("SOMEDIR").getFullName());
        assertEquals("SOMEDIR\\CHILDDIR\\", fs.getDirectoryInfo("SOMEDIR\\CHILDDIR").getFullName());
    }

    public void delete(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("Fred");
        assertEquals(1, fs.getRoot().getDirectories().size());
        fs.getRoot().getDirectories("Fred").get(0).delete();
        assertEquals(0, fs.getRoot().getDirectories().size());
    }

    public void deleteRecursive(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("Fred\\child");
        assertEquals(1, fs.getRoot().getDirectories().size());
        fs.getRoot().getDirectories("Fred").get(0).delete(true);
        assertEquals(0, fs.getRoot().getDirectories().size());
    }

    public void deleteRoot(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertThrows(IOException.class, () -> {
            fs.getRoot().delete();
        });
    }

    public void deleteNonEmpty_NonRecursive(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("Fred\\child");
        assertThrows(IOException.class, () -> {
            fs.getRoot().getDirectories("Fred").get(0).delete();
        });
    }

    public void createDeleteLeakTest(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        for (int i = 0; i < 2000; ++i) {
            fs.createDirectory("Fred");
            fs.getRoot().getDirectories("Fred").get(0).delete();
        }
        fs.createDirectory("SOMEDIR");
        DiscDirectoryInfo dirInfo = fs.getDirectoryInfo("SOMEDIR");
        assertNotNull(dirInfo);
        for (int i = 0; i < 2000; ++i) {
            fs.createDirectory("SOMEDIR\\Fred");
            dirInfo.getDirectories("Fred").get(0).delete();
        }
    }

    public void move(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("SOMEDIR\\CHILD\\GCHILD");
        fs.getDirectoryInfo("SOMEDIR\\CHILD").moveTo("NEWDIR");
        assertEquals(2, fs.getRoot().getDirectories().size());
        assertEquals(0, fs.getRoot().getDirectories("SOMEDIR").get(0).getDirectories().size());
    }

    public void extension(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertEquals("dir", fs.getDirectoryInfo("fred.dir").getExtension());
        assertEquals("", fs.getDirectoryInfo("fred").getExtension());
    }

    public void getDirectories(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("SOMEDIR\\CHILD\\GCHILD");
        fs.createDirectory("A.DIR");
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
        assertEquals("A.DIR\\", fs.getRoot().getDirectories("*.DIR", "AllDirectories").get(0).getFullName());
        assertEquals(1, fs.getRoot().getDirectories("GCHILD", "AllDirectories").size());
        assertEquals("SOMEDIR\\CHILD\\GCHILD\\", fs.getRoot().getDirectories("GCHILD", "AllDirectories").get(0).getFullName());
    }

    public void getDirectories_BadPath(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertThrows(FileNotFoundException.class, () -> {
            fs.getDirectories("\\baddir");
        });
    }

    public void getFiles(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("SOMEDIR\\CHILD\\GCHILD");
        fs.createDirectory("AAA.DIR");
        try (Stream s = fs.openFile("FOO.TXT", FileMode.Create)) {
        }
        try (Stream s = fs.openFile("SOMEDIR\\CHILD.TXT", FileMode.Create)) {
        }
        try (Stream s = fs.openFile("SOMEDIR\\FOO.TXT", FileMode.Create)) {
        }
        try (Stream s = fs.openFile("SOMEDIR\\CHILD\\GCHILD\\BAR.TXT", FileMode.Create)) {
        }
        assertEquals(1, fs.getRoot().getFiles().size());
        assertEquals("FOO.TXT", fs.getRoot().getFiles().get(0).getFullName());
        assertEquals(2, fs.getRoot().getDirectories("SOMEDIR").get(0).getFiles("*.TXT").size());
        assertEquals(4, fs.getRoot().getFiles("*.TXT", "AllDirectories").size());
        assertEquals(0, fs.getRoot().getFiles("*.DIR", "AllDirectories").size());
    }

    public void getFileSystemInfos(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("SOMEDIR\\CHILD\\GCHILD");
        fs.createDirectory("AAA.EXT");
        try (Stream s = fs.openFile("FOO.TXT", FileMode.Create)) {
        }
        try (Stream s = fs.openFile("SOMEDIR\\CHILD.EXT", FileMode.Create)) {
        }
        try (Stream s = fs.openFile("SOMEDIR\\FOO.TXT", FileMode.Create)) {
        }
        try (Stream s = fs.openFile("SOMEDIR\\CHILD\\GCHILD\\BAR.TXT", FileMode.Create)) {
        }
        assertEquals(3, fs.getRoot().getFileSystemInfos().size());
        assertEquals(1, fs.getRoot().getFileSystemInfos("*.EXT").size());
        assertEquals(2, fs.getRoot().getFileSystemInfos("*.?XT").size());
    }

    public void parent(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("SOMEDIR");
        assertEquals(fs.getRoot(), fs.getRoot().getDirectories("SOMEDIR").get(0).getParent());
    }

    public void parent_Root(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertNull(fs.getRoot().getParent());
    }

    public void creationTimeUtc(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("DIR");
        assertTrue(Instant.now().toEpochMilli() >= fs.getRoot().getDirectories("DIR").get(0).getCreationTimeUtc());
        assertTrue(Instant.now()
                .minus(Duration.ofSeconds(10))
                .toEpochMilli() <= fs.getRoot().getDirectories("DIR").get(0).getCreationTimeUtc());
    }

    public void creationTime(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("DIR");
        assertTrue(Instant.now().toEpochMilli() >= fs.getRoot().getDirectories("DIR").get(0).getCreationTime());
        assertTrue(Instant.now()
                .minus(Duration.ofSeconds(10))
                .toEpochMilli() <= fs.getRoot().getDirectories("DIR").get(0).getCreationTime());
    }

    public void lastAccessTime(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("DIR");
        DiscDirectoryInfo di = fs.getDirectoryInfo("DIR");
        long baseTime = Instant.now().minus(Duration.ofDays(2)).toEpochMilli();
        di.setLastAccessTime(baseTime);
        fs.createDirectory("DIR\\CHILD");
        assertTrue(baseTime < di.getLastAccessTime());
    }

    public void lastWriteTime(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("DIR");
        DiscDirectoryInfo di = fs.getDirectoryInfo("DIR");
        long baseTime = Instant.now().minus(Duration.ofMinutes(10)).toEpochMilli();
        di.setLastWriteTime(baseTime);
        fs.createDirectory("DIR\\CHILD");
        assertTrue(baseTime < di.getLastWriteTime());
    }

    public void equals(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertEquals(fs.getDirectoryInfo("foo"), fs.getDirectoryInfo("foo"));
    }

    public void rootBehaviour(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        // Not all file systems can modify the root directory, so we just make sure 'get' and 'no-op' change work.
        fs.getRoot().setAttributes(fs.getRoot().getAttributes());
        fs.getRoot().setCreationTimeUtc(fs.getRoot().getCreationTimeUtc());
        fs.getRoot().setLastAccessTimeUtc(fs.getRoot().getLastAccessTimeUtc());
        fs.getRoot().setLastWriteTimeUtc(fs.getRoot().getLastWriteTimeUtc());
    }
}
