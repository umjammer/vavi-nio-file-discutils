
package DiscUtils.Streams;

//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiSparseStreamOpenDelegate implements SparseStreamOpenDelegate {
//    public SparseStream invoke() {
//        List<SparseStreamOpenDelegate> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        SparseStreamOpenDelegate prev = null;
//        for (Object __dummyForeachVar0 : copy) {
//            SparseStreamOpenDelegate d = (SparseStreamOpenDelegate) __dummyForeachVar0;
//            if (prev != null)
//                prev.invoke();
//
//            prev = d;
//        }
//        return prev.invoke();
//    }
//
//    private List<SparseStreamOpenDelegate> _invocationList = new ArrayList<>();
//
//    public static SparseStreamOpenDelegate combine(SparseStreamOpenDelegate a, SparseStreamOpenDelegate b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiSparseStreamOpenDelegate ret = new __MultiSparseStreamOpenDelegate();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static SparseStreamOpenDelegate remove(SparseStreamOpenDelegate a, SparseStreamOpenDelegate b) {
//        if (a == null || b == null)
//            return a;
//
//        List<SparseStreamOpenDelegate> aInvList = a.getInvocationList();
//        List<SparseStreamOpenDelegate> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiSparseStreamOpenDelegate ret = new __MultiSparseStreamOpenDelegate();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<SparseStreamOpenDelegate> getInvocationList() {
//        return _invocationList;
//    }
//
//}
