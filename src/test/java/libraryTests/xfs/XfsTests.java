//

package libraryTests.xfs;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import discUtils.xfs.Context;
import discUtils.xfs.Inode;
import discUtils.xfs.InodeFormat;
import discUtils.xfs.SuperBlock;
import discUtils.xfs.Symlink;
import discUtils.xfs.XfsFileSystemOptions;

public class XfsTests {
    @Test
    public void canReadSymlink() throws Exception {
        Context context = new Context();
        context.setSuperBlock(new SuperBlock());
        XfsFileSystemOptions options = new XfsFileSystemOptions();
        options.setFileNameEncoding(StandardCharsets.UTF_8);
        context.setOptions(options);

        Inode inode = new Inode(1, context);
        inode.readFrom(getInodeBuffer(), 0);

        Symlink symlink = new Symlink(context, inode);
        assertEquals("init.d", symlink.getTargetPath());

        inode = new Inode(1, context);
        byte[] inodeBuffer = getInodeBuffer();
        inodeBuffer[0x6C] = 60; // garbage after first null byte
        inode.readFrom(inodeBuffer, 0);

        symlink = new Symlink(context, inode);
        assertEquals("init.d", symlink.getTargetPath());
    }

    private byte[] getInodeBuffer() throws Exception {
        byte[] inodeBuffer = new byte[0x70];
        inodeBuffer[0x5] = (byte) InodeFormat.Local.ordinal();
        inodeBuffer[0x3F] = 6; // Length (di_size)
        inodeBuffer[0x52] = 0; // Forkoff
        inodeBuffer[0x64] = (byte) 'i';
        inodeBuffer[0x65] = (byte) 'n';
        inodeBuffer[0x66] = (byte) 'i';
        inodeBuffer[0x67] = (byte) 't';
        inodeBuffer[0x68] = (byte) '.';
        inodeBuffer[0x69] = (byte) 'd';

        return inodeBuffer;
    }
}