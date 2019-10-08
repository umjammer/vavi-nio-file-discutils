
package DiscUtils.SquashFs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiAllocateId implements AllocateId {
//    public short invoke(int id) {
//        List<AllocateId> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        AllocateId prev = null;
//        for (AllocateId d : copy) {
//            if (prev != null)
//                prev.invoke(id);
//
//            prev = d;
//        }
//        return prev.invoke(id);
//    }
//
//    private List<AllocateId> _invocationList;
//
//    public static AllocateId combine(AllocateId a, AllocateId b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiAllocateId ret = new __MultiAllocateId();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static AllocateId remove(AllocateId a, AllocateId b) {
//        if (a == null || b == null)
//            return a;
//
//        List<AllocateId> aInvList = a.getInvocationList();
//        List<AllocateId> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiAllocateId ret = new __MultiAllocateId();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<AllocateId> getInvocationList() {
//        return _invocationList;
//    }
//}
