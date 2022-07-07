
package discUtils.powerShell.conpat;

public abstract class ItemCmdletProvider extends DriveCmdletProvider {

    protected ItemCmdletProvider() {
    }

    protected abstract void ClearItem(String path);

    protected abstract Object ClearItemDynamicParameters(String path);

    protected abstract String[] ExpandPath(String path);

    protected abstract void GetItem(String path);

    protected abstract Object GetItemDynamicParameters(String path);

    protected abstract void InvokeDefaultAction(String path);

    protected abstract Object InvokeDefaultActionDynamicParameters(String path);

    protected abstract boolean IsValidPath(String path);

    protected abstract boolean ItemExists(String path);

    protected abstract Object ItemExistsDynamicParameters(String path);

    protected abstract void SetItem(String path, Object value);

    protected abstract Object SetItemDynamicParameters(String path, Object value);
}
