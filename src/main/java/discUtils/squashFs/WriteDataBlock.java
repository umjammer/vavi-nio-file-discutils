
package discUtils.squashFs;

@FunctionalInterface
public interface WriteDataBlock {

    int invoke(byte[] buffer, int offset, int count);
}
