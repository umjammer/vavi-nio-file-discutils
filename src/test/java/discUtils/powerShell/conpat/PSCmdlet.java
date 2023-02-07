
package discUtils.powerShell.conpat;

import java.util.Collection;


public abstract class PSCmdlet extends Cmdlet {

    protected PSCmdlet() {}

    public PSEventManager getEvents() {
        return null;
    }

    public PSHost getHost() {
        return null;
    }

    public CommandInvocationIntrinsics getInvokeCommand() {
        return null;
    }

    public ProviderIntrinsics getInvokeProvider() {
        return null;
    }

    public JobManager getJobManager() {
        return null;
    }

    public JobRepository getJobRepository() {
        return null;
    }

    public InvocationInfo getMyInvocation() {
        return null;
    }

    public PagingParameters getPagingParameters() {
        return null;
    }

    public String getParameterSetName() {
        return null;
    }

    public SessionState getSessionState() {
        return null;
    }

    public abstract PathInfo CurrentProviderLocation(String providerId);

    public abstract Collection<String> GetResolvedProviderPathFromPSPath(String path, ProviderInfo[] provider);

    public abstract String GetUnresolvedProviderPathFromPSPath(String path);

    public abstract Object GetVariableValue(String name);

    public abstract Object GetVariableValue(String name, Object defaultValue);
}
