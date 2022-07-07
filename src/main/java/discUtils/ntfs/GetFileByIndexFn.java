
package discUtils.ntfs;

@FunctionalInterface
public interface GetFileByIndexFn {

    File invoke(long index);
}
