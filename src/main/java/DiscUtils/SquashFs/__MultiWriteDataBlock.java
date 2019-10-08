
package DiscUtils.SquashFs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiWriteDataBlock implements WriteDataBlock {
//    public int invoke(byte[] buffer, int offset, int count) {
//        List<WriteDataBlock> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        WriteDataBlock prev = null;
//        for (WriteDataBlock d : copy) {
//            if (prev != null)
//                prev.invoke(buffer, offset, count);
//
//            prev = d;
//        }
//        return prev.invoke(buffer, offset, count);
//    }
//
//    private List<WriteDataBlock> _invocationList = new ArrayList<>();
//
//    public static WriteDataBlock combine(WriteDataBlock a, WriteDataBlock b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiWriteDataBlock ret = new __MultiWriteDataBlock();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static WriteDataBlock remove(WriteDataBlock a, WriteDataBlock b) {
//        if (a == null || b == null)
//            return a;
//
//        List<WriteDataBlock> aInvList = a.getInvocationList();
//        List<WriteDataBlock> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiWriteDataBlock ret = new __MultiWriteDataBlock();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<WriteDataBlock> getInvocationList() {
//        return _invocationList;
//    }
//}
