
package DiscUtils.SquashFs;

@FunctionalInterface
public interface WriteDataBlock {

    int invoke(byte[] buffer, int offset, int count);

//    List<WriteDataBlock> getInvocationList();
}
