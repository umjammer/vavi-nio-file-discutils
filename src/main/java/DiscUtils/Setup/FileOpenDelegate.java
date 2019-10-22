
package DiscUtils.Setup;

import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.Stream;


@FunctionalInterface
public interface FileOpenDelegate {

    Stream invoke(String fileName, FileMode mode, FileAccess access, FileShare share);
}
