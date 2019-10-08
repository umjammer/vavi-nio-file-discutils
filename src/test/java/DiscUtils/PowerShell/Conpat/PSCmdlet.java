
package DiscUtils.PowerShell.Conpat;

public abstract class PSCmdlet extends Cmdlet {
    protected PSCmdlet();

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

    public string getParameterSetName() {
        return null;
    }

    public SessionState getSessionState() {
        return null;
    }

    public PathInfo CurrentProviderLocation(string providerId);

    public Collection<string> GetResolvedProviderPathFromPSPath(String path, ProviderInfo[] provider);

    public String GetUnresolvedProviderPathFromPSPath(string path);

    public Object GetVariableValue(string name);

    public Object GetVariableValue(string name, Object defaultValue);
}
