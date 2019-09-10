
package DiscUtils.Core;

import java.util.List;


public interface GeometryCalculation {
    Geometry invoke(long capacity);

    List<GeometryCalculation> getInvocationList();

}
