//
// Translated by CS2J (http://www.cs2j.com): 2019/07/15 9:43:07
//

package DiscUtils.Ntfs;

import java.util.List;

public interface GetDirectoryByIndexFn
{
    Directory invoke(long index);

    List<GetDirectoryByIndexFn> getInvocationList();

}

