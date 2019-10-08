//
// Translated by CS2J (http://www.cs2j.com): 2019/07/15 9:43:07
//

package DiscUtils.Ntfs;

import java.util.List;

public interface GetDirectoryByRefFn
{
    Directory invoke(FileRecordReference reference);

    List<GetDirectoryByRefFn> getInvocationList();

}


