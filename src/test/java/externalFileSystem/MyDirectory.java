
package externalFileSystem;

import java.util.ArrayList;
import java.util.List;

import discUtils.core.vfs.IVfsDirectory;
import dotnet4j.util.compat.StringUtilities;


class MyDirectory extends MyFile implements IVfsDirectory<MyDirEntry, MyFile> {
    private List<MyDirEntry> _entries = new ArrayList<>();

    public MyDirectory(MyDirEntry dirEntry, boolean isRoot) {
        super(dirEntry);
        _entries = new ArrayList<>();
        if (isRoot) {
            for (int i = 0; i < 4; ++i) {
                _entries.add(new MyDirEntry("DIR" + i, true));
            }
        }

        for (int i = 0; i < 6; ++i) {
            _entries.add(new MyDirEntry("FILE" + i, false));
        }
    }

    public List<MyDirEntry> getAllEntries() {
        return _entries;
    }

    public MyDirEntry getSelf() {
        return null;
    }

    public MyDirEntry getEntryByName(String name) {
        for (MyDirEntry entry : _entries) {
            if (StringUtilities.compare(name, entry.getFileName(), true) == 0) {
                return entry;
            }

        }
        return null;
    }

    public MyDirEntry createNewFile(String name) {
        throw new UnsupportedOperationException();
    }

}
