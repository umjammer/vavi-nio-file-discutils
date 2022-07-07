
package discUtils.squashFs;

@FunctionalInterface
public interface WriteFragment {

    int invoke(int length, int[] offset);
}
