
package externalFileSystem;

import java.util.EnumSet;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.IVfsFile;
import discUtils.streams.SparseMemoryBuffer;
import discUtils.streams.buffer.IBuffer;


class MyFile implements IVfsFile {
    private MyDirEntry _dirEntry;

    public MyFile(MyDirEntry dirEntry) {
        _dirEntry = dirEntry;
    }

    public long getLastAccessTimeUtc() {
        return _dirEntry.getLastAccessTimeUtc();
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return _dirEntry.getLastWriteTimeUtc();
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc() {
        return _dirEntry.getCreationTimeUtc();
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return _dirEntry.getFileAttributes();
    }

    public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    public long getFileLength() {
        return 10;
    }

    public IBuffer getFileContent() {
        SparseMemoryBuffer result = new SparseMemoryBuffer(10);
        result.write(0,
                     new byte[] {
                         0, 1, 2, 3, 4, 5, 6, 7, 8, 9
                     },
                     0,
                     10);
        return result;
    }
}
