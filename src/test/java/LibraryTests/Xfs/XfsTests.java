//

package LibraryTests.Xfs;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Xfs.Context;
import DiscUtils.Xfs.Inode;
import DiscUtils.Xfs.InodeFormat;
import DiscUtils.Xfs.SuperBlock;
import DiscUtils.Xfs.Symlink;
import DiscUtils.Xfs.XfsFileSystemOptions;

public class XfsTests {
    @Test
    public void canReadSymlink() throws Exception {
        Context context = new Context();
        context.setSuperBlock(new SuperBlock());
        XfsFileSystemOptions options = new XfsFileSystemOptions();
        options.setFileNameEncoding(Charset.forName("UTF8"));
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

    @Test
    private byte[] getInodeBuffer() throws Exception {
        byte[] inodeBuffer = new byte[0x70];
        inodeBuffer[0x5] = (byte) InodeFormat.Local.ordinal();
        inodeBuffer[0x3F] = 6; // Length (di_size)
        inodeBuffer[0x52] = 0; // Forkoff
        inodeBuffer[0X64] = (byte) 'i';
        inodeBuffer[0X65] = (byte) 'n';
        inodeBuffer[0X66] = (byte) 'i';
        inodeBuffer[0X67] = (byte) 't';
        inodeBuffer[0X68] = (byte) '.';
        inodeBuffer[0X69] = (byte) 'd';

        return inodeBuffer;
    }
}