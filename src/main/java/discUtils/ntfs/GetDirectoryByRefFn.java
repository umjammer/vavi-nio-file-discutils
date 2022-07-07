
package discUtils.ntfs;

public interface GetDirectoryByRefFn {

    Directory invoke(FileRecordReference reference);
}
