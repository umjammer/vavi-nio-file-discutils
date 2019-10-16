
package DiscUtils.Ntfs;

public interface GetDirectoryByRefFn {
    Directory invoke(FileRecordReference reference);

//    List<GetDirectoryByRefFn> getInvocationList();
}
