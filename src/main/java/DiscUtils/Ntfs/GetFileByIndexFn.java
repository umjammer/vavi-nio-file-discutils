
package DiscUtils.Ntfs;

@FunctionalInterface
public interface GetFileByIndexFn {

    File invoke(long index);
}
