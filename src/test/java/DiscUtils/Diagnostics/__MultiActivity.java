
package DiscUtils.Diagnostics;

//import java.util.ArrayList;
//import java.util.Map;
//import java.util.LinkedList;
//
//import DiscUtils.Core.DiscFileSystem;
//import DiscUtils.Core.IDiagnosticTraceable;
//import DiscUtils.Core.CoreCompat.ListSupport;


//public class __MultiActivity<TFileSystem extends DiscFileSystem & IDiagnosticTraceable> implements Activity<TFileSystem> {
//    public Object invoke(TFileSystem fs, Map<String, Object> context) {
//        List<Activity<TFileSystem>> copy = new ArrayList<Activity<TFileSystem>>(), members = this.getInvocationList();
//        synchronized (members) {
//            copy = new LinkedList<Activity<TFileSystem>>(members);
//        }
//        Activity<TFileSystem> prev = null;
//        for (Activity<TFileSystem> d : copy) {
//            if (prev != null)
//                prev.invoke(fs, context);
//
//            prev = d;
//        }
//        return prev.invoke(fs, context);
//    }
//
//    private List<Activity<TFileSystem>> _invocationList;
//


    /// </summary>
    /// <summary>
    /// The last verification report generated at a scheduled checkpoint.
    /// </summary>
    /// <summary>
    /// Flag set when a validation failure is observed, preventing further file system activity.
    /// </summary>
    /// <summary>
    /// The exception (if any) that indicated the file system was corrupt.
//    public List<Activity<TFileSystem>> getInvocationList() {
//        return _invocationList;
//    }
//
//}

/// </summary>
/// <summary>
/// The total number of events carried out before lock-down occured.
