
package DiscUtils.PowerShell.Conpat;

public abstract class ContainerCmdletProvider extends ItemCmdletProvider {
    protected ContainerCmdletProvider() {
    }

    protected abstract boolean convertPath(String path, String filter, String[] updatedPath, String[] updatedFilter);

    protected abstract void copyItem(String path, String copyPath, boolean recurse);

    protected abstract Object copyItemDynamicParameters(String path, String destination, boolean recurse);

    protected abstract void getChildItems(String path, boolean recurse);

    protected abstract Object getChildItemsDynamicParameters(String path, boolean recurse);

    protected abstract void getChildNames(String path, ReturnContainers returnContainers);

    protected abstract Object getChildNamesDynamicParameters(String path);

    protected abstract boolean hasChildItems(String path);

    protected abstract void newItem(String path, String itemTypeName, Object newItemValue);

    protected abstract Object newItemDynamicParameters(String path, String itemTypeName, Object newItemValue);

    protected abstract void removeItem(String path, boolean recurse);

    protected abstract Object removeItemDynamicParameters(String path, boolean recurse);

    protected abstract void renameItem(String path, String newName);

    protected abstract Object renameItemDynamicParameters(String path, String newName);
}
