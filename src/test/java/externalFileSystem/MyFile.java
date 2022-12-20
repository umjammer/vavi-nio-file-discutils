
package externalFileSystem;

import java.util.EnumSet;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.IVfsFile;
import discUtils.streams.SparseMemoryBuffer;
import discUtils.streams.buffer.IBuffer;


class MyFile implements IVfsFile {

    private MyDirEntry dirEntry;

    public MyFile(MyDirEntry dirEntry) {
        this.dirEntry = dirEntry;
    }

    @Override public long getLastAccessTimeUtc() {
        return dirEntry.getLastAccessTimeUtc();
    }

    @Override public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getLastWriteTimeUtc() {
        return dirEntry.getLastWriteTimeUtc();
    }

    @Override public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getCreationTimeUtc() {
        return dirEntry.getCreationTimeUtc();
    }

    @Override public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public EnumSet<FileAttributes> getFileAttributes() {
        return dirEntry.getFileAttributes();
    }

    @Override public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getFileLength() {
        return 10;
    }

    @Override public IBuffer getFileContent() {
        SparseMemoryBuffer result = new SparseMemoryBuffer(10);
        result.write(0, new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, 0, 10);
        return result;
    }
}
