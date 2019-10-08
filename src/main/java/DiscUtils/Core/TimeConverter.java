
package DiscUtils.Core;

@FunctionalInterface
public interface TimeConverter {

    long invoke(long time, boolean toUtc);

//    List<TimeConverter> getInvocationList();
}
