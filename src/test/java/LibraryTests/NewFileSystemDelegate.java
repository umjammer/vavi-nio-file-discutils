//

package LibraryTests;

import DiscUtils.Core.DiscFileSystem;

@FunctionalInterface
public interface NewFileSystemDelegate {

    DiscFileSystem invoke();

//    List<NewFileSystemDelegate> getInvocationList();
}
