
package DiscUtils.PowerShell.Conpat;

import java.util.Collection;


public abstract class CmdletProvider implements IResourceSupplier {
    protected CmdletProvider() {
    }

    public PSCredential getCredential() {
        return null;
    }

    public PSTransactionContext getCurrentPSTransaction() {
        return null;
    }

    public Collection<String> getExclude() {
        return null;
    }

    public String getFilter() {
        return null;
    }

    public SwitchParameter getForce() {
        return null;
    }

    public PSHost getHost() {
        return null;
    }

    public Collection<String> getInclude() {
        return null;
    }

    public CommandInvocationIntrinsics getInvokeCommand() {
        return null;
    }

    public ProviderIntrinsics getInvokeProvider() {
        return null;
    }

    public SessionState getSessionState() {
        return null;
    }

    public boolean getStopping() {
        return null;
    }

    protected Object getDynamicParameters() {
        return null;
    }

    protected PSDriveInfo getPSDriveInfo() {
        return null;
    }

    ProviderInfo getProviderInfo() {
        return null;
    }

    public abstract String GetResourceString(String baseName, String resourceId);

    public boolean ShouldContinue(String query, String caption) {
        return false;
    }

    public boolean ShouldContinue(String query, String caption, boolean[] yesToAll, boolean[] noToAll) {
        return false;
    }

    public boolean ShouldProcess(String target) {
        return false;
    }

    public boolean ShouldProcess(String target, String action) {
        return false;
    }

    public boolean ShouldProcess(String verboseDescription, String verboseWarning, String caption) {
        return false;
    }

    public boolean ShouldProcess(String verboseDescription,
                                 String verboseWarning,
                                 String caption,
                                 ShouldProcessReason[] shouldProcessReason) {
        return false;
    }

    public void ThrowTerminatingError(ErrorRecord errorRecord) {
    }

    public boolean TransactionAvailable() {
        return false;
    }

    public void WriteDebug(String text) {
    }

    public void WriteError(ErrorRecord errorRecord) {
    }

    public void WriteItemObject(Object item, String path, boolean isContainer) {
    }

    public void WriteProgress(ProgressRecord progressRecord) {
    }

    public void WritePropertyObject(Object propertyValue, String path) {
    }

    public void WriteSecurityDescriptorObject(ObjectSecurity securityDescriptor, String path) {
    }

    public void WriteVerbose(String text) {
    }

    public void WriteWarning(String text) {
    }

    protected abstract ProviderInfo Start(ProviderInfo providerInfo);

    protected abstract Object StartDynamicParameters();

    protected abstract void Stop();

    abstract void StopProcessing();
}
