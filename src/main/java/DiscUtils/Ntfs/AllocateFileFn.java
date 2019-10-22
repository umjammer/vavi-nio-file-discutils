
package DiscUtils.Ntfs;

import java.util.EnumSet;

@FunctionalInterface
public interface AllocateFileFn {

    File invoke(EnumSet<FileRecordFlags> flags);
}
