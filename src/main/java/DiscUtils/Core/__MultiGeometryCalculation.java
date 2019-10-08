
package DiscUtils.Core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import DiscUtils.Core.CoreCompat.ListSupport;


/**
 * Delegate for calculating a disk geometry from a capacity.
 */
public class __MultiGeometryCalculation implements GeometryCalculation {

    /**
     * @param capacity The disk capacity to convert.
     * @return The appropriate geometry for the disk.
     */
    public Geometry invoke(long capacity) {
        List<GeometryCalculation> copy = new ArrayList<>(), members = this.getInvocationList();
        synchronized (members) {
            copy = new LinkedList<>(members);
        }
        GeometryCalculation prev = null;
        for (GeometryCalculation d : copy) {
            if (prev != null)
                prev.invoke(capacity);

            prev = d;
        }
        return prev.invoke(capacity);
    }

    private List<GeometryCalculation> _invocationList = new ArrayList<>();

    public static GeometryCalculation combine(GeometryCalculation a, GeometryCalculation b) {
        if (a == null)
            return b;

        if (b == null)
            return a;

        __MultiGeometryCalculation ret = new __MultiGeometryCalculation();
        ret._invocationList = a.getInvocationList();
        ret._invocationList.addAll(b.getInvocationList());
        return ret;
    }

    public static GeometryCalculation remove(GeometryCalculation a, GeometryCalculation b) {
        if (a == null || b == null)
            return a;

        List<GeometryCalculation> aInvList = a.getInvocationList();
        List<GeometryCalculation> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
        if (aInvList == newInvList) {
            return a;
        } else {
            __MultiGeometryCalculation ret = new __MultiGeometryCalculation();
            ret._invocationList = newInvList;
            return ret;
        }
    }

    public List<GeometryCalculation> getInvocationList() {
        return _invocationList;
    }
}
