
package DiscUtils.Ntfs;

//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import DiscUtils.Core.CoreCompat.ListSupport;
//
//
//public class __MultiForgetFileFn implements ForgetFileFn {
//    public void invoke(File file) {
//        List<ForgetFileFn> copy = new ArrayList<>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<>(members);
//        }
//        for (ForgetFileFn d : copy) {
//            d.invoke(file);
//        }
//    }
//
//    private List<ForgetFileFn> _invocationList = new ArrayList<>();
//
//    public static ForgetFileFn combine(ForgetFileFn a, ForgetFileFn b) {
//        if (a == null)
//            return b;
//
//        if (b == null)
//            return a;
//
//        __MultiForgetFileFn ret = new __MultiForgetFileFn();
//        ret._invocationList = a.getInvocationList();
//        ret._invocationList.addAll(b.getInvocationList());
//        return ret;
//    }
//
//    public static ForgetFileFn remove(ForgetFileFn a, ForgetFileFn b) {
//        if (a == null || b == null)
//            return a;
//
//        List<ForgetFileFn> aInvList = a.getInvocationList();
//        List<ForgetFileFn> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//        if (aInvList == newInvList) {
//            return a;
//        } else {
//            __MultiForgetFileFn ret = new __MultiForgetFileFn();
//            ret._invocationList = newInvList;
//            return ret;
//        }
//    }
//
//    public List<ForgetFileFn> getInvocationList() {
//        return _invocationList;
//    }
//}
