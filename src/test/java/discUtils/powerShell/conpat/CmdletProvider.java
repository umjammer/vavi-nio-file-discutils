
package discUtils.powerShell.conpat;

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
        return false;
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

    @Override
    public abstract String getResourceString(String baseName, String resourceId);

    public boolean shouldContinue(String query, String caption) {
        return false;
    }

    public boolean shouldContinue(String query, String caption, boolean[] yesToAll, boolean[] noToAll) {
        return false;
    }

    public boolean shouldProcess(String target) {
        return false;
    }

    public boolean shouldProcess(String target, String action) {
        return false;
    }

    public boolean shouldProcess(String verboseDescription, String verboseWarning, String caption) {
        return false;
    }

    public boolean shouldProcess(String verboseDescription,
                                 String verboseWarning,
                                 String caption,
                                 ShouldProcessReason[] shouldProcessReason) {
        return false;
    }

    public void throwTerminatingError(ErrorRecord errorRecord) {
    }

    public boolean transactionAvailable() {
        return false;
    }

    public void writeDebug(String text) {
    }

    public void writeError(ErrorRecord errorRecord) {
    }

    public void writeItemObject(Object item, String path, boolean isContainer) {
    }

    public void writeProgress(ProgressRecord progressRecord) {
    }

    public void writePropertyObject(Object propertyValue, String path) {
    }

    public void writeSecurityDescriptorObject(ObjectSecurity securityDescriptor, String path) {
    }

    public void writeVerbose(String text) {
    }

    public void WriteWarning(String text) {
    }

    protected abstract ProviderInfo start(ProviderInfo providerInfo);

    protected abstract Object startDynamicParameters();

    protected abstract void stop();

    abstract void stopProcessing();
}
