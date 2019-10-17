//

package LibraryTests.Ext;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Core.DiscFileSystemInfo;
import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Ext.ExtFileSystem;
import LibraryTests.Helpers.Helpers;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.Stream;


public class ExtFileSystemTest {
    @Test
    public void loadFileSystem() throws Exception {
        try (Stream data = Helpers.loadDataFile(getClass(), "data.ext4.dat.gz");
                ExtFileSystem fs = new ExtFileSystem(data, new FileSystemParameters())) {
            List<DiscFileSystemInfo> fsis = fs.getRoot().getFileSystemInfos();
            Collections.sort(fsis, (s1, s2) -> s1.getName().compareTo(s2.getName()));
            final Iterator<Consumer<DiscFileSystemInfo>> i = Arrays.<Consumer<DiscFileSystemInfo>> asList(s -> {
                assertEquals("bar", s.getName());
                assertTrue(s.getAttributes().contains(FileAttributes.Directory));
            }, s -> {
                assertEquals("foo", s.getName());
                assertTrue(s.getAttributes().contains(FileAttributes.Directory));
            }, s -> {
                assertEquals("lost+found", s.getName());
                assertTrue(s.getAttributes().contains(FileAttributes.Directory));
            }).iterator();
            fsis.stream().forEach(f -> i.next().accept(f));
            assertTrue(fs.getRoot().getDirectories("foo").get(0).getFileSystemInfos().isEmpty());
            fsis = fs.getRoot().getDirectories("bar").get(0).getFileSystemInfos();
            Collections.sort(fsis, (s1, s2) -> s1.getName().compareTo(s2.getName()));
            final Iterator<Consumer<DiscFileSystemInfo>> j = Arrays.<Consumer<DiscFileSystemInfo>> asList(s -> {
                assertEquals("blah.txt", s.getName());
                assertTrue(s.getAttributes().contains(FileAttributes.Directory));
            }, s -> {
                assertEquals("testdir1", s.getName());
                assertTrue(s.getAttributes().contains(FileAttributes.Directory));
            }).iterator();
            fsis.stream().forEach(f -> j.next().accept(f));
            byte[] tmpData = Helpers.readAll(fs.openFile("bar\\blah.txt", FileMode.Open));

            assertEquals("hello world\n".getBytes(Charset.forName("ASCII")), tmpData);
            tmpData = Helpers.readAll(fs.openFile("bar\\testdir1\\test.txt", FileMode.Open));
            assertEquals("Mon Feb 11 19:54:14 UTC 2019\n".getBytes(Charset.forName("ASCII")), tmpData);
        }
    }
}
