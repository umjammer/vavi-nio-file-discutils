//

package LibraryTests;

import DiscUtils.Core.DiscFileSystem;

@FunctionalInterface
public interface NewFileSystemDelegate {

    DiscFileSystem invoke();
}
