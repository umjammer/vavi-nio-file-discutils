
package DiscUtils.Ntfs;

@FunctionalInterface
public interface ForgetFileFn {

    void invoke(File file);
}
