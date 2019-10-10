//

package LibraryTests.Ext;

import java.nio.charset.Charset;

import javax.sound.sampled.AudioFormat.Encoding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Ext.ExtFileSystem;
import LibraryTests.Helpers.Helpers;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.Stream;


public class ExtFileSystemTest {
    @Test
    public void loadFileSystem() throws Exception {
        try (Stream data = Helpers.loadDataFile("data.ext4.dat")) {
            try (ExtFileSystem fs = new ExtFileSystem(data, new FileSystemParameters())) {
                assertCollection(fs.getRoot().getFileSystemInfos().orderBy((s) -> {
                    return s.Name;
                }, (s) -> {
                    assertEquals("bar", s.Name);
                    assertTrue((s.Attributes & FileAttributes.Directory) != 0);
                }, (s) -> {
                    assertEquals("foo", s.Name);
                    assertTrue((s.Attributes & FileAttributes.Directory) != 0);
                }, (s) -> {
                    assertEquals("lost+found", s.Name);
                    assertTrue((s.Attributes & FileAttributes.Directory) != 0);
                }));
                assertTrue(fs.getRoot().getDirectories("foo").get(0).getFileSystemInfos().isEmpty());
                assertCollection(fs.getRoot().getDirectories("bar").get(0).getFileSystemInfos().orderBy((s) -> {
                    return s.Name;
                }, (s) -> {
                    assertEquals("blah.txt", s.Name);
                    assertTrue((s.Attributes & FileAttributes.Directory) == 0);
                }, (s) -> {
                    assertEquals("testdir1", s.Name);
                    assertTrue((s.Attributes & FileAttributes.Directory) != 0);
                }));
                Stream tmpData = fs.openFile("bar\\blah.txt", FileMode.Open).readAll();

                assertEquals("hello world\n".getBytes(Charset.forName("ASCII")), tmpData);
                tmpData = fs.openFile("bar\\testdir1\\test.txt", FileMode.Open).readAll();
                assertEquals("Mon Feb 11 19:54:14 UTC 2019\n".getBytes(Charset.forName("ASCII")), tmpData);
            }
        }
    }
}
