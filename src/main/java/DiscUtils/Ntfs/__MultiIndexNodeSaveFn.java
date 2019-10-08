
package DiscUtils.Ntfs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;


//public class __MultiIndexNodeSaveFn implements IndexNodeSaveFn {
//    public void invoke() {
//        List<IndexNodeSaveFn> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        for (IndexNodeSaveFn d : copy) {
//            d.invoke();
//        }
//    }
//
//    private List<IndexNodeSaveFn> _invocationList = new ArrayList<>();
//
//    public static IndexNodeSaveFn combine(IndexNodeSaveFn a, IndexNodeSaveFn b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiIndexNodeSaveFn ret = new __MultiIndexNodeSaveFn();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static IndexNodeSaveFn remove(IndexNodeSaveFn a, IndexNodeSaveFn b) {
//        if (a == null || b == null)
//            return a;
//
//        List<IndexNodeSaveFn> aInvList = a.getInvocationList();
//        List<IndexNodeSaveFn> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiIndexNodeSaveFn ret = new __MultiIndexNodeSaveFn();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<IndexNodeSaveFn> getInvocationList() {
//        return _invocationList;
//    }
//
//}
