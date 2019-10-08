
package DiscUtils.SquashFs;

@FunctionalInterface
public interface AllocateId {

    short invoke(int id);

//    List<AllocateId> getInvocationList();
}
