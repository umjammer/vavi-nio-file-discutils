
package DiscUtils.SquashFs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiAllocateInode implements AllocateInode {
//    public int invoke() {
//        List<AllocateInode> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        AllocateInode prev = null;
//        for (AllocateInode d : copy) {
//            if (prev != null)
//                prev.invoke();
//
//            prev = d;
//        }
//        return prev.invoke();
//    }
//
//    private List<AllocateInode> _invocationList;
//
//    public static AllocateInode combine(AllocateInode a, AllocateInode b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiAllocateInode ret = new __MultiAllocateInode();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static AllocateInode remove(AllocateInode a, AllocateInode b) {
//        if (a == null || b == null)
//            return a;
//
//        List<AllocateInode> aInvList = a.getInvocationList();
//        List<AllocateInode> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiAllocateInode ret = new __MultiAllocateInode();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<AllocateInode> getInvocationList() {
//        return _invocationList;
//    }
//}
