
package DiscUtils.Ntfs;

@FunctionalInterface
public interface GetFileByRefFn {

    File invoke(FileRecordReference reference);
}
