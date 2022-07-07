
package discUtils.ntfs;

@FunctionalInterface
public interface GetDirectoryByIndexFn {

    Directory invoke(long index);
}
