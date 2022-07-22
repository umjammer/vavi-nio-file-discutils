
package discUtils.hfsPlus;

import java.io.IOException;

import discUtils.core.vfs.IVfsSymlink;
import discUtils.streams.buffer.BufferStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.StreamReader;


public class Symlink extends File implements IVfsSymlink<DirEntry, File> {
    private String _targetPath;

    public Symlink(Context context, CatalogNodeId nodeId, CommonCatalogFileInfo catalogInfo) {
        super(context, nodeId, catalogInfo);
    }

    public String getTargetPath() {
        if (_targetPath == null) {
            try (BufferStream stream = new BufferStream(getFileContent(), FileAccess.Read)) {
                try (StreamReader reader = new StreamReader(stream)) {
                    _targetPath = reader.readToEnd();
                    _targetPath = _targetPath.replace('/', java.io.File.separatorChar);
                }
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        return _targetPath;
    }
}
