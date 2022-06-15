
package DiscUtils.PowerShell.Conpat;

public abstract class NavigationCmdletProvider extends ContainerCmdletProvider {
    protected NavigationCmdletProvider() {}

    protected abstract String getChildName(String path);

    protected abstract String getParentPath(String path, String root);

    protected abstract boolean isItemContainer(String path);

    protected abstract String makePath(String parent, String child);

    protected abstract String makePath(String parent, String child, boolean childIsLeaf);

    protected abstract void moveItem(String path, String destination);

    protected abstract Object moveItemDynamicParameters(String path, String destination);

    protected abstract String normalizeRelativePath(String path, String basePath);
}
