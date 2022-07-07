//

package libraryTests;

import discUtils.core.DiscFileSystem;

@FunctionalInterface
public interface NewFileSystemDelegate {

    DiscFileSystem invoke();
}
