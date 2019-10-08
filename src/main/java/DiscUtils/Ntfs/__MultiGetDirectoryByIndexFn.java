
package DiscUtils.Ntfs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import DiscUtils.Core.CoreCompat.ListSupport;


public class __MultiGetDirectoryByIndexFn implements GetDirectoryByIndexFn {
    public Directory invoke(long index) {
        List<GetDirectoryByIndexFn> copy = new ArrayList<>(), members = this.getInvocationList();
        synchronized (members) {
            copy = new LinkedList<>(members);
        }
        GetDirectoryByIndexFn prev = null;
        for (GetDirectoryByIndexFn d : copy) {
            if (prev != null)
                prev.invoke(index);

            prev = d;
        }
        return prev.invoke(index);
    }

    private List<GetDirectoryByIndexFn> _invocationList = new ArrayList<>();

    public static GetDirectoryByIndexFn combine(GetDirectoryByIndexFn a, GetDirectoryByIndexFn b) {
        if (a == null)
            return b;

        if (b == null)
            return a;

        __MultiGetDirectoryByIndexFn ret = new __MultiGetDirectoryByIndexFn();
        ret._invocationList = a.getInvocationList();
        ret._invocationList.addAll(b.getInvocationList());
        return ret;
    }

    public static GetDirectoryByIndexFn remove(GetDirectoryByIndexFn a, GetDirectoryByIndexFn b) {
        if (a == null || b == null)
            return a;

        List<GetDirectoryByIndexFn> aInvList = a.getInvocationList();
        List<GetDirectoryByIndexFn> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
        if (aInvList == newInvList) {
            return a;
        } else {
            __MultiGetDirectoryByIndexFn ret = new __MultiGetDirectoryByIndexFn();
            ret._invocationList = newInvList;
            return ret;
        }
    }

    public List<GetDirectoryByIndexFn> getInvocationList() {
        return _invocationList;
    }

}
