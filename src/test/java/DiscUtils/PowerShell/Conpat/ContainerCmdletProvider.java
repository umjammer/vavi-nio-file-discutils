
package DiscUtils.PowerShell.Conpat;

public abstract class ContainerCmdletProvider extends ItemCmdletProvider {
    protected ContainerCmdletProvider() {
    }

    protected abstract boolean ConvertPath(String path, String filter, String[] updatedPath, String[] updatedFilter);

    protected abstract void CopyItem(String path, String copyPath, boolean recurse);

    protected abstract Object CopyItemDynamicParameters(String path, String destination, boolean recurse);

    protected abstract void GetChildItems(String path, boolean recurse);

    protected abstract Object GetChildItemsDynamicParameters(String path, boolean recurse);

    protected abstract void GetChildNames(String path, ReturnContainers returnContainers);

    protected abstract Object GetChildNamesDynamicParameters(String path);

    protected abstract boolean HasChildItems(String path);

    protected abstract void NewItem(String path, String itemTypeName, Object newItemValue);

    protected abstract Object NewItemDynamicParameters(String path, String itemTypeName, Object newItemValue);

    protected abstract void RemoveItem(String path, boolean recurse);

    protected abstract Object RemoveItemDynamicParameters(String path, boolean recurse);

    protected abstract void RenameItem(String path, String newName);

    protected abstract Object RenameItemDynamicParameters(String path, String newName);
}
