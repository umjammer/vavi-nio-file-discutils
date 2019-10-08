
package DiscUtils.SquashFs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiReadMetaBlock implements ReadMetaBlock {
//    public Metablock invoke(long pos) {
//        List<ReadMetaBlock> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        ReadMetaBlock prev = null;
//        for (ReadMetaBlock d : copy) {
//            if (prev != null)
//                prev.invoke(pos);
//
//            prev = d;
//        }
//        return prev.invoke(pos);
//    }
//
//    private List<ReadMetaBlock> _invocationList = new ArrayList<>();
//
//    public static ReadMetaBlock combine(ReadMetaBlock a, ReadMetaBlock b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiReadMetaBlock ret = new __MultiReadMetaBlock();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static ReadMetaBlock remove(ReadMetaBlock a, ReadMetaBlock b) {
//        if (a == null || b == null)
//            return a;
//
//        List<ReadMetaBlock> aInvList = a.getInvocationList();
//        List<ReadMetaBlock> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiReadMetaBlock ret = new __MultiReadMetaBlock();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<ReadMetaBlock> getInvocationList() {
//        return _invocationList;
//    }
//}
