
package DiscUtils.SquashFs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//import DiscUtils.Streams.Block.Block;
//
//
//public class __MultiReadBlock implements ReadBlock {
//    public Block invoke(long pos, int diskLen) {
//        List<ReadBlock> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        ReadBlock prev = null;
//        for (ReadBlock d : copy) {
//            if (prev != null)
//                prev.invoke(pos, diskLen);
//
//            prev = d;
//        }
//        return prev.invoke(pos, diskLen);
//    }
//
//    private List<ReadBlock> _invocationList;
//
//    public static ReadBlock combine(ReadBlock a, ReadBlock b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiReadBlock ret = new __MultiReadBlock();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static ReadBlock remove(ReadBlock a, ReadBlock b) {
//        if (a == null || b == null)
//            return a;
//
//        List<ReadBlock> aInvList = a.getInvocationList();
//        List<ReadBlock> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiReadBlock ret = new __MultiReadBlock();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<ReadBlock> getInvocationList() {
//        return _invocationList;
//    }
//}
