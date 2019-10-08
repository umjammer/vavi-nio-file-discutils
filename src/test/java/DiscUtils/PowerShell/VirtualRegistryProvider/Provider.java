//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package DiscUtils.PowerShell.VirtualRegistryProvider;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import DiscUtils.PowerShell.Utilities;
import DiscUtils.Registry.RegistryHive;
import DiscUtils.Registry.RegistryKey;
import DiscUtils.Registry.RegistryValueType;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.Stream;


public final class Provider extends NavigationCmdletProvider implements IDynamicPropertyCmdletProvider {
    private static final String DefaultValueName = "(default)";

    protected PSDriveInfo newDrive(PSDriveInfo drive) {
        NewDriveParameters dynParams = DynamicParameters instanceof NewDriveParameters ? (NewDriveParameters) DynamicParameters
                                                                                       : (NewDriveParameters) null;
        if (drive == null) {
            writeError(new ErrorRecord(new NullPointerException(nameof(drive)),
                                       "NullDrive",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return null;
        }

        if (String.IsNullOrEmpty(drive.Root)) {
            writeError(new ErrorRecord(new IllegalArgumentException("drive"), "NoRoot", ErrorCategory.InvalidArgument, drive));
            return null;
        }

        String[] mountPaths = drive.Root.split('!');
        if (mountPaths.length < 1 || mountPaths.length > 2) {
            writeError(new ErrorRecord(new IllegalArgumentException("drive"),
                                       "InvalidRoot",
                                       ErrorCategory.InvalidArgument,
                                       drive));
            return null;
        }

        String filePath = mountPaths[0];
        String relPath = mountPaths.length > 1 ? mountPaths[1] : "";
        Stream hiveStream = null;
        FileAccess access = dynParams.getReadWrite().IsPresent ? FileAccess.ReadWrite : FileAccess.Read;
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        filePath = Utilities.resolvePsPath(SessionState, filePath);
        hiveStream = Utilities.openPsPath(SessionState, filePath, access, share);
        if (hiveStream == null) {
            writeError(new ErrorRecord(new IllegalArgumentException("drive"),
                                       "InvalidRoot",
                                       ErrorCategory.InvalidArgument,
                                       drive));
            return null;
        } else {
            return new VirtualRegistryPSDriveInfo(drive,
                                                  makePath(Utilities.normalizePath(filePath + "!"),
                                                           Utilities.normalizePath(relPath)),
                                                  hiveStream);
        }
    }

    protected Object newDriveDynamicParameters() {
        return new NewDriveParameters();
    }

    protected PSDriveInfo removeDrive(PSDriveInfo drive) {
        if (drive == null) {
            writeError(new ErrorRecord(new NullPointerException(nameof(drive)),
                                       "NullDrive",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return null;
        }

        VirtualRegistryPSDriveInfo vrDrive = drive instanceof VirtualRegistryPSDriveInfo ? (VirtualRegistryPSDriveInfo) drive
                                                                                         : (VirtualRegistryPSDriveInfo) null;
        if (vrDrive == null) {
            writeError(new ErrorRecord(new IllegalArgumentException("invalid type of drive"),
                                       "BadDrive",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return null;
        }

        vrDrive.close();
        return vrDrive;
    }

    protected void getItem(String path) {
        RegistryKey key = findItemByPath(path);
        writeKey(path, key);
    }

    protected Object getItemDynamicParameters(String path) {
        return null;
    }

    protected void setItem(String path, Object value) {
        throw new UnsupportedOperationException();
    }

    protected boolean itemExists(String path) {
        return findItemByPath(path) != null;
    }

    protected boolean isValidPath(String path) {
        throw new UnsupportedOperationException();
    }

    protected void getChildItems(String path, boolean recurse) {
        RegistryKey key = findItemByPath(path);
        for (Object subKeyName : key.getSubKeyNames()) {
            writeKey(makePath(path, subKeyName), key.openSubKey(subKeyName));
        }
    }

    protected void getChildNames(String path, ReturnContainers returnContainers) {
        RegistryKey key = findItemByPath(path);
        for (Object subKeyName : key.getSubKeyNames()) {
            writeItemObject(subKeyName, makePath(path, subKeyName), true);
        }
    }

    protected boolean hasChildItems(String path) {
        RegistryKey key = findItemByPath(path);
        return key.getSubKeyCount() != 0;
    }

    protected void removeItem(String path, boolean recurse) {
        String parentPath = getParentPath(path, null);
        RegistryKey parentKey = findItemByPath(parentPath);
        if (recurse) {
            parentKey.DeleteSubKeyTree(getChildName(path));
        } else {
            parentKey.DeleteSubKey(getChildName(path));
        }
    }

    protected void newItem(String path, String itemTypeName, Object newItemValue) {
        String parentPath = getParentPath(path, null);
        RegistryKey parentKey = findItemByPath(parentPath);
        WriteItemObject(parentKey.CreateSubKey(getChildName(path)), path, true);
    }

    protected void renameItem(String path, String newName) {
        throw new UnsupportedOperationException();
    }

    protected void copyItem(String path, String copyPath, boolean recurse) {
        throw new UnsupportedOperationException();
    }

    protected boolean isItemContainer(String path) {
        return true;
    }

    protected String makePath(String parent, String child) {
        return Utilities.NormalizePath(super.makePath(Utilities.denormalizePath(parent), Utilities.denormalizePath(child)));
    }

    public void clearProperty(String path, Collection<String> propertyToClear) {
        PSObject propVal = new PSObject();
        boolean foundProp = false;
        RegistryKey key = findItemByPath(path);
        for (String valueName : key.getValueNames()) {
            String propName = valueName;
            if (valueName == null && valueName.isEmpty()) {
                propName = DefaultValueName;
            }

            if (isMatch(propName, propertyToClear)) {
                RegistryValueType type = key.getValueType(valueName);
                Object newVal = defaultRegistryTypeValue(type);
                key.setValue(valueName, newVal);
                propVal.Properties.add(new PSNoteProperty(propName, newVal));
                foundProp = true;
            }

        }
        if (foundProp) {
            WritePropertyObject(propVal, path);
        }
    }

    public Object clearPropertyDynamicParameters(String path, Collection<String> propertyToClear) {
        return null;
    }

    public void getProperty(String path, Collection<String> providerSpecificPickList) {
        PSObject propVal = new PSObject();
        boolean foundProp = false;
        RegistryKey key = findItemByPath(path);
        for (String valueName : key.getValueNames()) {
            String propName = valueName;
            if (valueName == null || valueName.isEmpty()) {
                propName = DefaultValueName;
            }

            if (isMatch(propName, providerSpecificPickList)) {
                propVal.Properties.add(new PSNoteProperty(propName, key.getValue(valueName)));
                foundProp = true;
            }
        }
        if (foundProp) {
            WritePropertyObject(propVal, path);
        }
    }

    public Object getPropertyDynamicParameters(String path, Collection<String> providerSpecificPickList) {
        return null;
    }

    public void setProperty(String path, PSObject propertyValue) {
        PSObject propVal = new PSObject();
        RegistryKey key = findItemByPath(path);
        if (key == null) {
            writeError(new ErrorRecord(new IllegalArgumentException("path"),
                                       "NoSuchRegistryKey",
                                       ErrorCategory.ObjectNotFound,
                                       path));
        }

        for (Object prop : propertyValue.Properties) {
            key.SetValue(prop.Name, prop.Value);
        }
    }

    public Object setPropertyDynamicParameters(String path, PSObject propertyValue) {
        return null;
    }

    public void copyProperty(String sourcePath, String sourceProperty, String destinationPath, String destinationProperty) {
        throw new UnsupportedOperationException();
    }

    public Object copyPropertyDynamicParameters(String sourcePath,
                                                String sourceProperty,
                                                String destinationPath,
                                                String destinationProperty) {
        return null;
    }

    public void moveProperty(String sourcePath, String sourceProperty, String destinationPath, String destinationProperty) {
        throw new UnsupportedOperationException();
    }

    public Object movePropertyDynamicParameters(String sourcePath,
                                                String sourceProperty,
                                                String destinationPath,
                                                String destinationProperty) {
        return null;
    }

    public void newProperty(String path, String propertyName, String propertyTypeName, Object value) {
        RegistryKey key = findItemByPath(path);
        if (key == null) {
            WriteError(new ErrorRecord(new IllegalArgumentException("path"),
                                       "NoSuchRegistryKey",
                                       ErrorCategory.ObjectNotFound,
                                       path));
        }

        RegistryValueType type;
        type = RegistryValueType.None;
        if (propertyTypeName != null && !propertyTypeName.isEmpty()) {
            type = RegistryValueType.valueOf(propertyTypeName);
        }

        if (propertyName.compareTo(DefaultValueName) == 0) {
            propertyName = "";
        }

        key.setValue(propertyName, value != null ? value : defaultRegistryTypeValue(type), type);
    }

    public Object newPropertyDynamicParameters(String path, String propertyName, String propertyTypeName, Object value) {
        return null;
    }

    public void removeProperty(String path, String propertyName) {
        RegistryKey key = findItemByPath(path);
        if (key == null) {
            writeError(new ErrorRecord(new IllegalArgumentException("path"),
                                       "NoSuchRegistryKey",
                                       ErrorCategory.ObjectNotFound,
                                       path));
        }

        if (propertyName.compareTo(DefaultValueName) == 0) {
            propertyName = "";
        }

        key.deleteValue(propertyName);
    }

    public Object removePropertyDynamicParameters(String path, String propertyName) {
        return null;
    }

    public void renameProperty(String path, String sourceProperty, String destinationProperty) {
        throw new UnsupportedOperationException();
    }

    public Object renamePropertyDynamicParameters(String path, String sourceProperty, String destinationProperty) {
        return null;
    }

    private VirtualRegistryPSDriveInfo getDriveInfo() {
        return PSDriveInfo instanceof VirtualRegistryPSDriveInfo ? (VirtualRegistryPSDriveInfo) PSDriveInfo
                                                                 : (VirtualRegistryPSDriveInfo) null;
    }

    private RegistryHive getHive() {
        VirtualRegistryPSDriveInfo driveInfo = getDriveInfo();
        return (driveInfo != null) ? driveInfo.getHive() : null;
    }

    private RegistryKey findItemByPath(String path) {
        String filePath;
        String relPath;
        int mountSepIdx = path.indexOf('!');
        if (mountSepIdx < 0) {
            filePath = path;
            relPath = "";
        } else {
            filePath = path.substring(0, mountSepIdx);
            relPath = path.substring(mountSepIdx + 1);
            if (relPath.length() > 0 && relPath.charAt(0) == '\\') {
                relPath = relPath.substring(1);
            }

        }
        RegistryHive hive = getHive();
        if (hive == null) {
            throw new UnsupportedOperationException("Accessing registry hives outside of a mounted drive");
        }

        return hive.getRoot().openSubKey(relPath);
    }

    private void writeKey(String path, RegistryKey key) {
        if (key == null) {
            return;
        }

        PSObject psObj = PSObject.AsPSObject(key);
        List<String> valueNames = key.getValueNames();
        for (int i = 0; i < valueNames.size(); ++i) {
            if (valueNames.get(i) == null || valueNames.get(i).isEmpty()) {
                valueNames.add(i, DefaultValueName);
            }
        }
        psObj.Properties.add(new PSNoteProperty("Property", valueNames));
        writeItemObject(psObj, path.replaceAll("(^\\*|\\*$", ""), true);
    }

    private boolean isMatch(String valueName, Collection<String> filters) {
        if (filters == null || filters.size() == 0) {
            return true;
        }

        for (String filter : filters) {
            if (filter.indexOf('*') != -1 || filter.indexOf('?') != -1) {
                Pattern pattern = Pattern.compile("^" + filter.replace("*", ".*").replace("?", ".") + "$"); // TODO escape
                if (pattern.matcher(valueName).matches()) {
                    return true;
                }

            } else if (filter.compareTo(valueName) == 0) {
                return true;
            }

        }
        return false;
    }

    private static Object defaultRegistryTypeValue(RegistryValueType type) {
        if (type.equals(RegistryValueType.Binary) || type.equals(RegistryValueType.None)) {
            return new byte[] {};
        } else if (type.equals(RegistryValueType.Dword) ||
                   type.equals(RegistryValueType.DwordBigEndian)) {
            return 0;
        } else if (type.equals(RegistryValueType.QWord)) {
            return 0L;
        } else if (type.equals(RegistryValueType.String) ||
                   type.equals(RegistryValueType.ExpandString)) {
            return "";
        } else if (type.equals(RegistryValueType.MultiString)) {
            return new String[] {};
        }

        return null;
    }
}
