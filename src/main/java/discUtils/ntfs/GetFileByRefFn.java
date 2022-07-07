
package discUtils.ntfs;

@FunctionalInterface
public interface GetFileByRefFn {

    File invoke(FileRecordReference reference);
}
