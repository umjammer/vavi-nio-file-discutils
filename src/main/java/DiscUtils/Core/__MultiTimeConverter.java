
package DiscUtils.Core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import DiscUtils.Core.CoreCompat.ListSupport;


/**
 * Converts a time to/from UTC.
 * 
 * @param time The time to convert.
 * @param toUtc
 *            {@code true}
 *            to convert FAT time to UTC,
 *            {@code false}
 *            to convert UTC to FAT time.
 * @return The converted time.
 */
public class __MultiTimeConverter implements TimeConverter {
    public long invoke(long time, boolean toUtc) {
        List<TimeConverter> copy = new ArrayList<>(), members = this.getInvocationList();
        synchronized (members) {
            copy = new LinkedList<>(members);
        }
        TimeConverter prev = null;
        for (Object __dummyForeachVar0 : copy) {
            TimeConverter d = (TimeConverter) __dummyForeachVar0;
            if (prev != null)
                prev.invoke(time, toUtc);

            prev = d;
        }
        return prev.invoke(time, toUtc);
    }

    private List<TimeConverter> _invocationList = new ArrayList<>();

    public static TimeConverter combine(TimeConverter a, TimeConverter b) {
        if (a == null)
            return b;

        if (b == null)
            return a;

        __MultiTimeConverter ret = new __MultiTimeConverter();
        ret._invocationList = a.getInvocationList();
        ret._invocationList.addAll(b.getInvocationList());
        return ret;
    }

    public static TimeConverter remove(TimeConverter a, TimeConverter b) {
        if (a == null || b == null)
            return a;

        List<TimeConverter> aInvList = a.getInvocationList();
        List<TimeConverter> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
        if (aInvList == newInvList) {
            return a;
        } else {
            __MultiTimeConverter ret = new __MultiTimeConverter();
            ret._invocationList = newInvList;
            return ret;
        }
    }

    public List<TimeConverter> getInvocationList() {
        return _invocationList;
    }

}
