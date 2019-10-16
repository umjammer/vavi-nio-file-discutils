
package DiscUtils.Ntfs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiGetFileByRefFn implements GetFileByRefFn {
//    public File invoke(FileRecordReference reference) {
//        List<GetFileByRefFn> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        GetFileByRefFn prev = null;
//        for (GetFileByRefFn d : copy) {
//            if (prev != null)
//                prev.invoke(reference);
//
//            prev = d;
//        }
//        return prev.invoke(reference);
//    }
//
//    private List<GetFileByRefFn> _invocationList;
//
//    public static GetFileByRefFn combine(GetFileByRefFn a, GetFileByRefFn b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiGetFileByRefFn ret = new __MultiGetFileByRefFn();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static GetFileByRefFn remove(GetFileByRefFn a, GetFileByRefFn b) {
//        if (a == null || b == null)
//            return a;
//
//        List<GetFileByRefFn> aInvList = a.getInvocationList();
//        List<GetFileByRefFn> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiGetFileByRefFn ret = new __MultiGetFileByRefFn();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<GetFileByRefFn> getInvocationList() {
//        return _invocationList;
//    }
//}
