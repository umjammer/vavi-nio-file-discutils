
package DiscUtils.Fat;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;


//public class __MultiFirstClusterChangedDelegate implements FirstClusterChangedDelegate {
//    public void invoke(int cluster) {
//        List<FirstClusterChangedDelegate> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        for (FirstClusterChangedDelegate d : copy) {
//            d.invoke(cluster);
//        }
//    }
//
//    private List<FirstClusterChangedDelegate> _invocationList = new ArrayList<>();
//
//    public static FirstClusterChangedDelegate combine(FirstClusterChangedDelegate a, FirstClusterChangedDelegate b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiFirstClusterChangedDelegate ret = new __MultiFirstClusterChangedDelegate();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static FirstClusterChangedDelegate remove(FirstClusterChangedDelegate a, FirstClusterChangedDelegate b) {
//        if (a == null || b == null)
//            return a;
//
//        List<FirstClusterChangedDelegate> aInvList = a.getInvocationList();
//        List<FirstClusterChangedDelegate> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiFirstClusterChangedDelegate ret = new __MultiFirstClusterChangedDelegate();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<FirstClusterChangedDelegate> getInvocationList() {
//        return _invocationList;
//    }
//}
