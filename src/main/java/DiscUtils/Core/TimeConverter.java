
package DiscUtils.Core;

import java.util.List;


public interface TimeConverter {
    long invoke(long time, boolean toUtc);

    List<TimeConverter> getInvocationList();

}
