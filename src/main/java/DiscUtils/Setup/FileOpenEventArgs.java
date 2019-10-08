
package DiscUtils.Setup;

import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.Stream;

public class FileOpenEventArgs // extends EventArgs
{
    private FileOpenDelegate _opener;

    public FileOpenEventArgs(String fileName,
                             FileMode mode,
                             FileAccess access,
                             FileShare share,
            FileOpenDelegate opener) {
        setFileName(fileName);
        setFileMode(mode);
        setFileAccess(access);
        setFileShare(share);
        _opener = opener;
    }

    private String __FileName;

    public String getFileName() {
        return __FileName;
    }

    public void setFileName(String value) {
        __FileName = value;
    }

    private FileMode __FileMode;

    public FileMode getFileMode() {
        return __FileMode;
    }

    public void setFileMode(FileMode value) {
        __FileMode = value;
    }

    private FileAccess __FileAccess;

    public FileAccess getFileAccess() {
        return __FileAccess;
    }

    public void setFileAccess(FileAccess value) {
        __FileAccess = value;
    }

    private FileShare __FileShare;

    public FileShare getFileShare() {
        return __FileShare;
    }

    public void setFileShare(FileShare value) {
        __FileShare = value;
    }

    private Stream __Result;

    public Stream getResult() {
        return __Result;
    }

    public void setResult(Stream value) {
        __Result = value;
    }

    public Stream getFileStream() {
        return _opener.invoke(getFileName(), getFileMode(), getFileAccess(), getFileShare());
    }

}
