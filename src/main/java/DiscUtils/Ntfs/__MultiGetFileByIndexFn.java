
package DiscUtils.Ntfs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import DiscUtils.Core.CoreCompat.ListSupport;


public class __MultiGetFileByIndexFn implements GetFileByIndexFn {
    public File invoke(long index) {
        List<GetFileByIndexFn> copy = new ArrayList<>(), members = this.getInvocationList();
        synchronized (members) {
            copy = new LinkedList<>(members);
        }
        GetFileByIndexFn prev = null;
        for (GetFileByIndexFn d : copy) {
            if (prev != null)
                prev.invoke(index);

            prev = d;
        }
        return prev.invoke(index);
    }

    private List<GetFileByIndexFn> _invocationList = new ArrayList<>();

    public static GetFileByIndexFn combine(GetFileByIndexFn a, GetFileByIndexFn b) {
        if (a == null)
            return b;

        if (b == null)
            return a;

        __MultiGetFileByIndexFn ret = new __MultiGetFileByIndexFn();
        ret._invocationList = a.getInvocationList();
        ret._invocationList.addAll(b.getInvocationList());
        return ret;
    }

    public static GetFileByIndexFn remove(GetFileByIndexFn a, GetFileByIndexFn b) {
        if (a == null || b == null)
            return a;

        List<GetFileByIndexFn> aInvList = a.getInvocationList();
        List<GetFileByIndexFn> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
        if (aInvList == newInvList) {
            return a;
        } else {
            __MultiGetFileByIndexFn ret = new __MultiGetFileByIndexFn();
            ret._invocationList = newInvList;
            return ret;
        }
    }

    public List<GetFileByIndexFn> getInvocationList() {
        return _invocationList;
    }

}
