
package DiscUtils.Ntfs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import DiscUtils.Core.CoreCompat.ListSupport;


public class __MultiGetDirectoryByRefFn implements GetDirectoryByRefFn {
    public Directory invoke(FileRecordReference reference) {
        List<GetDirectoryByRefFn> copy = new ArrayList<>(), members = this.getInvocationList();
        synchronized (members) {
            copy = new LinkedList<>(members);
        }
        GetDirectoryByRefFn prev = null;
        for (GetDirectoryByRefFn d : copy) {
            if (prev != null)
                prev.invoke(reference);

            prev = d;
        }
        return prev.invoke(reference);
    }

    private List<GetDirectoryByRefFn> _invocationList = new ArrayList<>();

    public static GetDirectoryByRefFn combine(GetDirectoryByRefFn a, GetDirectoryByRefFn b) {
        if (a == null)
            return b;

        if (b == null)
            return a;

        __MultiGetDirectoryByRefFn ret = new __MultiGetDirectoryByRefFn();
        ret._invocationList = a.getInvocationList();
        ret._invocationList.addAll(b.getInvocationList());
        return ret;
    }

    public static GetDirectoryByRefFn remove(GetDirectoryByRefFn a, GetDirectoryByRefFn b) {
        if (a == null || b == null)
            return a;

        List<GetDirectoryByRefFn> aInvList = a.getInvocationList();
        List<GetDirectoryByRefFn> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
        if (aInvList == newInvList) {
            return a;
        } else {
            __MultiGetDirectoryByRefFn ret = new __MultiGetDirectoryByRefFn();
            ret._invocationList = newInvList;
            return ret;
        }
    }

    public List<GetDirectoryByRefFn> getInvocationList() {
        return _invocationList;
    }

}
