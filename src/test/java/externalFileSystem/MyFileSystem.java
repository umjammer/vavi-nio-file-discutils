
package externalFileSystem;

import java.io.IOException;

import discUtils.core.DiscFileSystemOptions;
import discUtils.core.vfs.VfsFileSystem;


class MyFileSystem extends VfsFileSystem<MyDirEntry, MyFile, MyDirectory, MyContext> {
    public MyFileSystem() {
        super(new DiscFileSystemOptions());
        this.setContext(new MyContext());
        this.setRootDirectory(new MyDirectory(new MyDirEntry("", true), true));
    }

    public String getVolumeLabel() {
        return "Volume Label";
    }

    protected MyFile convertDirEntryToFile(MyDirEntry dirEntry) {
        if (dirEntry.isDirectory()) {
            return new MyDirectory(dirEntry, false);
        } else {
            return new MyFile(dirEntry);
        }
    }

    public String getFriendlyName() {
        return "My File System";
    }

    public boolean canWrite() {
        return false;
    }

    public long getSize() throws IOException {
        throw new UnsupportedOperationException();
    }

    public long getUsedSpace() throws IOException {
        throw new UnsupportedOperationException();
    }

    public long getAvailableSpace() throws IOException {
        throw new UnsupportedOperationException();
    }
}
