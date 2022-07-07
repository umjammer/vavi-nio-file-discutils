
package discUtils.setup;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


@FunctionalInterface
public interface FileOpenDelegate {

    Stream invoke(String fileName, FileMode mode, FileAccess access, FileShare share);
}
