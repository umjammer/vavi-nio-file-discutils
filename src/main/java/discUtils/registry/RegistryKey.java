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

package discUtils.registry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import discUtils.core.internal.Utilities;
import discUtils.streams.util.MathUtilities;
import dotnet4j.security.accessControl.RegistrySecurity;
import dotnet4j.util.compat.StringUtilities;
import dotnet4j.win32.RegistryValueOptions;
import vavi.util.ByteUtil;


/**
 * A key within a registry hive.
 */
public final class RegistryKey {

    private static final String FS = java.io.File.separator;

    private final KeyNodeCell cell;

    private final RegistryHive hive;

    public RegistryKey(RegistryHive hive, KeyNodeCell cell) {
        this.hive = hive;
        this.cell = cell;
    }

    /**
     * Gets the class name of this registry key. Class name is rarely used.
     */
    public String getClassName() {
        if (cell.classNameIndex > 0) {
            return new String(hive.rawCellData(cell.classNameIndex, cell.classNameLength), StandardCharsets.UTF_16LE);
        }

        return null;
    }

    /**
     * Gets the flags of this registry key.
     */
    public EnumSet<RegistryKeyFlags> getFlags() {
        return cell.flags;
    }

    /**
     * Gets the name of this key.
     */
    public String getName() {
        RegistryKey parent = getParent();
        if (parent != null && !parent.getFlags().contains(RegistryKeyFlags.Root)) {
            return parent.getName() + FS + cell.name;
        }
        return cell.name;
    }

    /**
     * Gets the parent key, or {@code null} if this is the root key.
     */
    public RegistryKey getParent() {
        if (!cell.flags.contains(RegistryKeyFlags.Root)) {
            return new RegistryKey(hive, hive.getCell(cell.parentIndex));
        }
        return null;
    }

    /**
     * Gets the number of child keys.
     */
    public int getSubKeyCount() {
        return cell.numSubKeys;
    }

    /**
     * Gets an enumerator over all sub child keys.
     */
    public List<RegistryKey> getSubKeys() {
        List<RegistryKey> result = new ArrayList<>();
        if (cell.numSubKeys != 0) {
            ListCell list = hive.getCell(cell.subKeysIndex);
            for (KeyNodeCell key : list.enumerateKeys()) {
                result.add(new RegistryKey(hive, key));
            }
        }
        return result;
    }

    /**
     * Gets the time the key was last modified.
     */
    public long getTimestamp() {
        return cell.timestamp;
    }

    /**
     * Gets the number of values in this key.
     */
    public int getValueCount() {
        return cell.numValues;
    }

    /**
     * Gets an enumerator over all values in this key.
     */
    private List<RegistryValue> getValues() {
        List<RegistryValue> result = new ArrayList<>();
        if (cell.numValues != 0) {
            byte[] valueList = hive.rawCellData(cell.valueListIndex, cell.numValues * 4);
            for (int i = 0; i < cell.numValues; ++i) {
                int valueIndex = ByteUtil.readLeInt(valueList, i * 4);
                result.add(new RegistryValue(hive, hive.getCell(valueIndex)));
            }
        }
        return result;
    }

    /**
     * Gets the Security Descriptor applied to the registry key.
     *
     * @return The security descriptor as a RegistrySecurity instance.
     */
    public RegistrySecurity getAccessControl() {
        if (cell.securityIndex > 0) {
            SecurityCell secCell = hive.getCell(cell.securityIndex);
            return secCell.getSecurityDescriptor();
        }

        return null;
    }

    /**
     * Gets the names of all child sub keys.
     *
     * @return The names of the sub keys.
     */
    public List<String> getSubKeyNames() {
        List<String> names = new ArrayList<>();
        if (cell.numSubKeys != 0) {
            ListCell cell = hive.getCell(this.cell.subKeysIndex);
            cell.enumerateKeys(names);
        }

        return names;
    }

    /**
     * Gets a named value stored within this key.
     *
     * The mapping from registry type of .NET type is as follows:
     *
     * <pre>
     * Value Type:     .NET type
     *
     * String:         string
     * ExpandString:   string
     * Link:           string
     * DWord:          uint
     * DWordBigEndian: uint
     * MultiString:    string[]
     * QWord:          ulong
     * </pre>
     *
     * @param name The name of the value to retrieve.
     * @return The value as a .NET object.
     */
    public Object getValue(String name) {
        return getValue(name, null, RegistryValueOptions.None);
    }

    /**
     * Gets a named value stored within this key.
     *
     * The mapping from registry type of .NET type is as follows:
     *
     * <pre>
     * Value Type:     .NET type
     *
     * String:         string
     * ExpandString:   string
     * Link:           string
     * DWord:          uint
     * DWordBigEndian: uint
     * MultiString:    string[]
     * QWord:          ulong
     * </pre>
     *
     * @param name The name of the value to retrieve.
     * @param defaultValue The default value to return, if no existing value is
     *            stored.
     * @return The value as a .NET object.
     */
    public Object getValue(String name, Object defaultValue) {
        return getValue(name, defaultValue, RegistryValueOptions.None);
    }

    /**
     * Gets a named value stored within this key.
     *
     * The mapping from registry type of .NET type is as follows:
     *
     * <pre>
     * Value Type:     .NET type
     *
     * String:         string
     * ExpandString:   string
     * Link:           string
     * DWord:          uint
     * DWordBigEndian: uint
     * MultiString:    string[]
     * QWord:          ulong
     * </pre>
     *
     * @param name The name of the value to retrieve.
     * @param defaultValue The default value to return, if no existing value is
     *            stored.
     * @param options flags controlling how the value is processed before it's
     *            returned.
     * @return The value as a .NET object.
     */
    public Object getValue(String name, Object defaultValue, RegistryValueOptions options) {
        RegistryValue regVal = getRegistryValue(name);
        if (regVal != null) {
            if (regVal.getDataType() == RegistryValueType.ExpandString &&
                options != RegistryValueOptions.DoNotExpandEnvironmentNames) {
                return Utilities.expandEnvironmentVariables((String) regVal.getValue());
            }

            return regVal.getValue();
        }

        return defaultValue;
    }

    /**
     * Sets a named value stored within this key.
     *
     * @param name The name of the value to store.
     * @param value The value to store.
     */
    public void setValue(String name, Object value) {
        setValue(name, value, RegistryValueType.None);
    }

    /**
     * Sets a named value stored within this key.
     *
     * @param name The name of the value to store.
     * @param value The value to store.
     * @param valueType The registry type of the data.
     */
    public void setValue(String name, Object value, RegistryValueType valueType) {
        RegistryValue valObj = getRegistryValue(name);
        if (valObj == null) {
            valObj = addRegistryValue(name);
        }

        valObj.setValue(value, valueType);
    }

    /**
     * Deletes a named value stored within this key.
     *
     * @param name The name of the value to delete.
     */
    public void deleteValue(String name) {
        deleteValue(name, true);
    }

    /**
     * Deletes a named value stored within this key.
     *
     * @param name The name of the value to delete.
     * @param throwOnMissingValue Throws ArgumentException if {@code name}
     *            doesn't exist.
     */
    public void deleteValue(String name, boolean throwOnMissingValue) {
        boolean foundValue = false;
        if (cell.numValues != 0) {
            byte[] valueList = hive.rawCellData(cell.valueListIndex, cell.numValues * 4);
            int i = 0;
            while (i < cell.numValues) {
                int valueIndex = ByteUtil.readLeInt(valueList, i * 4);
                ValueCell valueCell = hive.getCell(valueIndex);
//Debug.println(valueCell.getName() + ", " + name);
                if (StringUtilities.compare(valueCell.getName(), name, true) == 0) {
                    foundValue = true;
                    hive.freeCell(valueIndex);
                    cell.numValues--;
                    hive.updateCell(cell, false);
                    break;
                }

                ++i;
            }
            // Move following value's to fill gap
            if (i < cell.numValues) {
                while (i < cell.numValues) {
                    int valueIndex = ByteUtil.readLeInt(valueList, (i + 1) * 4);
                    ByteUtil.writeLeInt(valueIndex, valueList, i * 4);
                    ++i;
                }
                hive.writeRawCellData(cell.valueListIndex, valueList, 0, cell.numValues * 4);
            }

            // TODO: Update maxbytes for value name and value content if this
            // was the
            // largest value for either.
            // Windows seems to repair this info, if not accurate, though.
        }

        if (throwOnMissingValue && !foundValue) {
            throw new IllegalArgumentException("No such value: " + name);
        }
    }

    /**
     * Gets the type of a named value.
     *
     * @param name The name of the value to inspect.
     * @return The value's type.
     */
    public RegistryValueType getValueType(String name) {
        RegistryValue regVal = getRegistryValue(name);
        if (regVal != null) {
            return regVal.getDataType();
        }

        return RegistryValueType.None;
    }

    /**
     * Gets the names of all values in this key.
     *
     * @return An array of strings containing the value names.
     */
    public List<String> getValueNames() {
        List<String> names = new ArrayList<>();
        for (RegistryValue value : getValues()) {
            names.add(value.getName());
        }
        return names;
    }

    /**
     * Creates or opens a subkey.
     *
     * @param subkey The relative path the the subkey.
     * @return The subkey.
     */
    public RegistryKey createSubKey(String subkey) {
        if (subkey == null || subkey.isEmpty()) {
            return this;
        }

        String[] split = subkey.split(StringUtilities.escapeForRegex(FS), 2);
        int cellIndex = findSubKeyCell(split[0]);
        if (cellIndex < 0) {
            KeyNodeCell newKeyCell = new KeyNodeCell(split[0], cell.getIndex());
            newKeyCell.securityIndex = cell.securityIndex;
            referenceSecurityCell(newKeyCell.securityIndex);
            hive.updateCell(newKeyCell, true);
            linkSubKey(split[0], newKeyCell.getIndex());
            if (split.length == 1) {
                return new RegistryKey(hive, newKeyCell);
            }

            return new RegistryKey(hive, newKeyCell).createSubKey(split[1]);
        }

        KeyNodeCell cell = hive.getCell(cellIndex);
        if (split.length == 1) {
            return new RegistryKey(hive, cell);
        }

        return new RegistryKey(hive, cell).createSubKey(split[1]);
    }

    /**
     * Opens a sub key.
     *
     * @param path The relative path to the sub key.
     * @return The sub key, or {@code null} if not found.
     */
    public RegistryKey openSubKey(String path) {
        if (path == null || path.isEmpty()) {
            return this;
        }

        String[] split = path.split(StringUtilities.escapeForRegex(FS), 2);
//Debug.println(StringUtil.paramString(split));
        int cellIndex = findSubKeyCell(split[0]);
        if (cellIndex < 0) {
            return null;
        }

        KeyNodeCell cell = hive.getCell(cellIndex);
        if (split.length == 1) {
            return new RegistryKey(hive, cell);
        }

        return new RegistryKey(hive, cell).openSubKey(split[1]);
    }

    /**
     * Deletes a subkey and any child subkeys recursively. The string subkey is
     * not case-sensitive.
     *
     * @param subkey The subkey to delete.
     */
    public void deleteSubKeyTree(String subkey) {
        RegistryKey subKeyObj = openSubKey(subkey);
        if (subKeyObj == null) {
            return;
        }

        if (subKeyObj.getFlags().contains(RegistryKeyFlags.Root)) {
            throw new IllegalArgumentException("Attempt to delete root key");
        }

        for (String child : subKeyObj.getSubKeyNames()) {
            subKeyObj.deleteSubKeyTree(child);
        }

        deleteSubKey(subkey);
    }

    /**
     * Deletes the specified subkey. The string subkey is not case-sensitive.
     *
     * @param subkey The subkey to delete.
     */
    public void deleteSubKey(String subkey) {
        deleteSubKey(subkey, true);
    }

    /**
     * Deletes the specified subkey. The string subkey is not case-sensitive.
     *
     * @param subkey The subkey to delete.
     * @param throwOnMissingSubKey {@code true} to throw an argument exception
     *            if {@code subkey} doesn't exist.
     */
    public void deleteSubKey(String subkey, boolean throwOnMissingSubKey) {
        if (subkey == null || subkey.isEmpty()) {
            throw new IllegalArgumentException("Invalid SubKey");
        }

        String[] split = subkey.split(StringUtilities.escapeForRegex(FS), 2);
        int subkeyCellIndex = findSubKeyCell(split[0]);
        if (subkeyCellIndex < 0) {
            if (throwOnMissingSubKey) {
                throw new IllegalArgumentException("No such SubKey");
            }

            return;
        }

        KeyNodeCell subkeyCell = hive.getCell(subkeyCellIndex);
        if (split.length == 1) {
            if (subkeyCell.numSubKeys != 0) {
                throw new UnsupportedOperationException("The registry key has subkeys");
            }

            if (subkeyCell.classNameIndex != -1) {
                hive.freeCell(subkeyCell.classNameIndex);
                subkeyCell.classNameIndex = -1;
                subkeyCell.classNameLength = 0;
            }

            if (subkeyCell.securityIndex != -1) {
                dereferenceSecurityCell(subkeyCell.securityIndex);
                subkeyCell.securityIndex = -1;
            }

            if (subkeyCell.subKeysIndex != -1) {
                freeSubKeys(subkeyCell);
            }

            if (subkeyCell.valueListIndex != -1) {
                freeValues(subkeyCell);
            }

            unlinkSubKey(subkey);
            hive.freeCell(subkeyCellIndex);
            hive.updateCell(cell, false);
        } else {
            new RegistryKey(hive, subkeyCell).deleteSubKey(split[1], throwOnMissingSubKey);
        }
    }

    private RegistryValue getRegistryValue(String name) {
        if (name != null && name.length() == 0) {
            name = null;
        }

        if (cell.numValues != 0) {
            byte[] valueList = hive.rawCellData(cell.valueListIndex, cell.numValues * 4);
            for (int i = 0; i < cell.numValues; ++i) {
                int valueIndex = ByteUtil.readLeInt(valueList, i * 4);
                ValueCell cell = hive.getCell(valueIndex);
//Debug.println(name + ", " + cell);
                if (StringUtilities.compare(cell.getName(), name, true) == 0) {
                    return new RegistryValue(hive, cell);
                }
            }
        }

        return null;
    }

    private RegistryValue addRegistryValue(String name) {
        byte[] valueList = hive.rawCellData(cell.valueListIndex, cell.numValues * 4);
        if (valueList == null) {
            valueList = new byte[0];
        }

        int insertIdx = 0;
        while (insertIdx < cell.numValues) {
            int valueCellIndex = ByteUtil.readLeInt(valueList, insertIdx * 4);
            ValueCell cell = hive.getCell(valueCellIndex);
            if (StringUtilities.compare(name, cell.getName(), true) < 0) {
                break;
            }

            ++insertIdx;
        }
        // Allocate a new value cell (note hive.UpdateCell does actual
        // allocation).
        ValueCell valueCell = new ValueCell(name);
        hive.updateCell(valueCell, true);

        // Update the value list, re-allocating if necessary
        byte[] newValueList = new byte[cell.numValues * 4 + 4];
        System.arraycopy(valueList, 0, newValueList, 0, insertIdx * 4);
        ByteUtil.writeLeInt(valueCell.getIndex(), newValueList, insertIdx * 4);
        System.arraycopy(valueList, insertIdx * 4, newValueList, insertIdx * 4 + 4, (cell.numValues - insertIdx) * 4);
        if (cell.valueListIndex == -1 || !hive.writeRawCellData(cell.valueListIndex, newValueList, 0, newValueList.length)) {
            int newListCellIndex = hive.allocateRawCell(MathUtilities.roundUp(newValueList.length, 8));
            hive.writeRawCellData(newListCellIndex, newValueList, 0, newValueList.length);

            if (cell.valueListIndex != -1) {
                hive.freeCell(cell.valueListIndex);
            }

            cell.valueListIndex = newListCellIndex;
        }

        // Record the new value and save this cell
        cell.numValues++;
        hive.updateCell(cell, false);

        // Finally, set the data in the value cell
//Debug.println(valueCell);
        return new RegistryValue(hive, valueCell);
    }

    private int findSubKeyCell(String name) {
        if (cell.numSubKeys != 0) {
            ListCell listCell = hive.getCell(cell.subKeysIndex);
            int[] cellIndex = new int[1];
            if (listCell.findKey(name, cellIndex) == 0) {
                return cellIndex[0];
            }
        }

        return -1;
    }

    private void linkSubKey(String name, int cellIndex) {
        if (cell.subKeysIndex == -1) {
            SubKeyHashedListCell newListCell = new SubKeyHashedListCell(hive, "lf");
            newListCell.add(name, cellIndex);
            hive.updateCell(newListCell, true);
            cell.numSubKeys = 1;
            cell.subKeysIndex = newListCell.getIndex();
        } else {
            ListCell list = hive.getCell(cell.subKeysIndex);
            cell.subKeysIndex = list.linkSubKey(name, cellIndex);
            cell.numSubKeys++;
        }
        hive.updateCell(cell, false);
    }

    private void unlinkSubKey(String name) {
        if (cell.subKeysIndex == -1 || cell.numSubKeys == 0) {
            throw new UnsupportedOperationException("No subkey list");
        }

        ListCell list = hive.getCell(cell.subKeysIndex);
        cell.subKeysIndex = list.unlinkSubKey(name);
        cell.numSubKeys--;
    }

    private void referenceSecurityCell(int cellIndex) {
        SecurityCell sc = hive.getCell(cellIndex);
        sc.setUsageCount(sc.getUsageCount() + 1);
        hive.updateCell(sc, false);
    }

    private void dereferenceSecurityCell(int cellIndex) {
        SecurityCell sc = hive.getCell(cellIndex);
        sc.setUsageCount(sc.getUsageCount() - 1);
        if (sc.getUsageCount() == 0) {
            SecurityCell prev = hive.getCell(sc.getPreviousIndex());
            prev.setNextIndex(sc.getNextIndex());
            hive.updateCell(prev, false);

            SecurityCell next = hive.getCell(sc.getNextIndex());
            next.setPreviousIndex(sc.getPreviousIndex());
            hive.updateCell(next, false);

            hive.freeCell(cellIndex);
        } else {
            hive.updateCell(sc, false);
        }
    }

    private void freeValues(KeyNodeCell cell) {
        if (cell.numValues != 0 && cell.valueListIndex != -1) {
            byte[] valueList = hive.rawCellData(cell.valueListIndex, cell.numValues * 4);

            for (int i = 0; i < cell.numValues; ++i) {
                int valueIndex = ByteUtil.readLeInt(valueList, i * 4);
                hive.freeCell(valueIndex);
            }

            hive.freeCell(cell.valueListIndex);
            cell.valueListIndex = -1;
            cell.numValues = 0;
            cell.maxValDataBytes = 0;
            cell.maxValNameBytes = 0;
        }
    }

    private void freeSubKeys(KeyNodeCell subkeyCell) {
        if (subkeyCell.subKeysIndex == -1) {
            throw new UnsupportedOperationException("No subkey list");
        }

        Cell list = hive.getCell(subkeyCell.subKeysIndex);
        if (list instanceof SubKeyIndirectListCell) {
            SubKeyIndirectListCell indirectList = (SubKeyIndirectListCell) list;
            // foreach (int listIndex in indirectList.CellIndexes)
            for (int i = 0; i < indirectList.getCellIndexes().size(); ++i) {
                int listIndex = indirectList.getCellIndexes().get(i);
                hive.freeCell(listIndex);
            }
        }

        hive.freeCell(list.getIndex());
    }

    public String toString() {
        return cell.toString();
    }
}
