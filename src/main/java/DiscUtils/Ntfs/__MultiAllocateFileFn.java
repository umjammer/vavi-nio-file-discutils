
package DiscUtils.Ntfs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiAllocateFileFn implements AllocateFileFn {
//    public File invoke(FileRecordFlags flags) {
//        List<AllocateFileFn> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        AllocateFileFn prev = null;
//        for (AllocateFileFn d : copy) {
//            if (prev != null)
//                prev.invoke(flags);
//
//            prev = d;
//        }
//        return prev.invoke(flags);
//    }
//
//    private List<AllocateFileFn> _invocationList = new ArrayList<>();
//
//    public static AllocateFileFn combine(AllocateFileFn a, AllocateFileFn b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiAllocateFileFn ret = new __MultiAllocateFileFn();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static AllocateFileFn remove(AllocateFileFn a, AllocateFileFn b) {
//        if (a == null || b == null)
//            return a;
//
//        List<AllocateFileFn> aInvList = a.getInvocationList();
//        List<AllocateFileFn> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiAllocateFileFn ret = new __MultiAllocateFileFn();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<AllocateFileFn> getInvocationList() {
//        return _invocationList;
//    }
//
//}
