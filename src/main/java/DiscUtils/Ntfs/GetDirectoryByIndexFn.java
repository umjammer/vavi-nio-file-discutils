
package DiscUtils.Ntfs;

@FunctionalInterface
public interface GetDirectoryByIndexFn {

    Directory invoke(long index);
}
