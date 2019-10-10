//

package LibraryTests.Xfs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Xfs.Context;
import DiscUtils.Xfs.Inode;
import DiscUtils.Xfs.InodeFormat;
import DiscUtils.Xfs.Symlink;

public class XfsTests {
    public void canReadSymlink() throws Exception {
        Context context = new Context();
        Inode inode = new Inode(1, context);
        inode.readFrom(getInodeBuffer(), 0);
        Symlink symlink = new Symlink(context, inode);
        assertEquals("init.d", symlink.getTargetPath());
        inode = new Inode(1, context);
        byte[] inodeBuffer = getInodeBuffer();
        inodeBuffer[0x6C] = 60;
        //garbage after first null byte
        inode.readFrom(inodeBuffer, 0);
        symlink = new Symlink(context, inode);
        assertEquals("init.d", symlink.getTargetPath());
    }

    private byte[] getInodeBuffer() throws Exception {
        byte[] inodeBuffer = new byte[0x70];
        inodeBuffer[0x5] = (byte) InodeFormat.Local.ordinal();
        inodeBuffer[0x3F] = 6;
        //Length (di_size)
        inodeBuffer[0x52] = 0;
        //Forkoff
        inodeBuffer[0X64] = (byte) 'i';
        inodeBuffer[0X65] = (byte) 'n';
        inodeBuffer[0X66] = (byte) 'i';
        inodeBuffer[0X67] = (byte) 't';
        inodeBuffer[0X68] = (byte) '.';
        inodeBuffer[0X69] = (byte) 'd';
        return inodeBuffer;
    }
}