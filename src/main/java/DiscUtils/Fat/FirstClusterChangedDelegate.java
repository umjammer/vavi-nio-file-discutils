
package DiscUtils.Fat;

@FunctionalInterface
public interface FirstClusterChangedDelegate {

    void invoke(int cluster);

//    List<FirstClusterChangedDelegate> getInvocationList();
}
