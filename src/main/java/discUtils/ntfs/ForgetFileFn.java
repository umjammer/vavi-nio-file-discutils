
package discUtils.ntfs;

@FunctionalInterface
public interface ForgetFileFn {

    void invoke(File file);
}
