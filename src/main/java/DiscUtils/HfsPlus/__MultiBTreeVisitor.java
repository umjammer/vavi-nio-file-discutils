
package DiscUtils.HfsPlus;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;


//public class __MultiBTreeVisitor<Key extends BTreeKey> implements BTreeVisitor<Key> {
//    public int invoke(Key key, byte[] data) {
//        List<BTreeVisitor<Key>> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<BTreeVisitor<Key>>(members);
//        }
//        BTreeVisitor<Key> prev = null;
//        for (BTreeVisitor<Key> d : copy) {
//            if (prev != null)
//                prev.invoke(key, data);
//
//            prev = d;
//        }
//        return prev.invoke(key, data);
//    }
//
//    private List<BTreeVisitor<Key>> _invocationList = new ArrayList<BTreeVisitor<Key>>();
//
//    public static <Key> BTreeVisitor<Key> combine(BTreeVisitor<Key> a, BTreeVisitor<Key> b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiBTreeVisitor<Key> ret = new __MultiBTreeVisitor<>();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static <Key> BTreeVisitor<Key> remove(BTreeVisitor<Key> a, BTreeVisitor<Key> b) {
//        if (a == null || b == null)
//            return a;
//
//        List<BTreeVisitor<Key>> aInvList = a.getInvocationList();
//        List<BTreeVisitor<Key>> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiBTreeVisitor<Key> ret = new __MultiBTreeVisitor<>();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<BTreeVisitor<Key>> getInvocationList() {
//        return _invocationList;
//    }
//}
