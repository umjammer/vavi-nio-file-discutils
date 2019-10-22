
package DiscUtils.SquashFs;

@FunctionalInterface
public interface WriteFragment {

    int invoke(int length, int[] offset);
}
