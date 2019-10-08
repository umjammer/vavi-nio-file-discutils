
package DiscUtils.Diagnostics;

import java.util.Map;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.IDiagnosticTraceable;

@FunctionalInterface
public interface Activity<TFileSystem extends DiscFileSystem & IDiagnosticTraceable> {

    Object invoke(TFileSystem fs, Map<String, Object> context);

//    List<Activity<TFileSystem>> getInvocationList();
}
