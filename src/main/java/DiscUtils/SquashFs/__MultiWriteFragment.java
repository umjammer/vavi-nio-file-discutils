
package DiscUtils.SquashFs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiWriteFragment implements WriteFragment {
//
//    public int invoke(int length, int[] offset) {
//        List<WriteFragment> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        WriteFragment prev = null;
//        for (WriteFragment d : copy) {
//            if (prev != null) {
//                int[] out = new int[1];
//                prev.invoke(length, out);
//                offset[0] = out[0];
//            }
//
//            prev = d;
//        }
//        int[] out = new int[1];
//        int result = prev.invoke(length, out);
//        offset[0] = out[0];
//        return result;
//    }
//
//    private List<WriteFragment> _invocationList = new ArrayList<>();
//
//    public static WriteFragment combine(WriteFragment a, WriteFragment b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiWriteFragment ret = new __MultiWriteFragment();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static WriteFragment remove(WriteFragment a, WriteFragment b) {
//        if (a == null || b == null)
//            return a;
//
//        List<WriteFragment> aInvList = a.getInvocationList();
//        List<WriteFragment> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiWriteFragment ret = new __MultiWriteFragment();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<WriteFragment> getInvocationList() {
//        return _invocationList;
//    }
//}
