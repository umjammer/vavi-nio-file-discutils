
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

    @Override
    public String getVolumeLabel() {
        return "Volume Label";
    }

    @Override
    protected MyFile convertDirEntryToFile(MyDirEntry dirEntry) {
        if (dirEntry.isDirectory()) {
            return new MyDirectory(dirEntry, false);
        } else {
            return new MyFile(dirEntry);
        }
    }

    @Override
    public String getFriendlyName() {
        return "My File System";
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public long getSize() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getUsedSpace() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAvailableSpace() throws IOException {
        throw new UnsupportedOperationException();
    }
}
