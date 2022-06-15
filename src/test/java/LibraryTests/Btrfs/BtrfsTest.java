//

package LibraryTests.Btrfs;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import DiscUtils.Btrfs.BtrfsFileSystem;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


public class BtrfsTest {

    @Test
    public void ignoreInvalidLabelData() throws Exception {
        try (Stream ms = new MemoryStream()) {
            ms.setPosition(0x20000);
            //set fs length
            ms.writeByte((byte) 1);
            ms.setPosition(0x10000L + 0x12b);
            //Label offset
            byte[] b = new byte[0x100];
            IntStream.range(1, 0x100).forEach(i -> b[i] = (byte) i);
            ms.write(b, 0, 0x100);
            //create label without null terminator
            ms.seek(0, SeekOrigin.Begin);
            IOException ex = assertThrows(IOException.class, () -> new BtrfsFileSystem(ms));
            assertEquals("Invalid Superblock Magic", ex.getMessage());
        }
    }

    @Test
    public void emptyStreamIsNoValidBtrfs() throws Exception {
        try (Stream ms = new MemoryStream()) {
            IOException ex = assertThrows(IOException.class, () -> new BtrfsFileSystem(ms));
            assertEquals("No Superblock detected", ex.getMessage());
        }
    }
}
