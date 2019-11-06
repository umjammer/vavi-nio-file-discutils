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

package DiscUtils.Registry;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import dotnet4j.io.compat.StringUtilities;
import dotnet4j.security.accessControl.RegistrySecurity;
import dotnet4j.win32.RegistryValueOptions;


/**
 * A key within a registry hive.
 */
public final class RegistryKey {
    private final KeyNodeCell _cell;

    private final RegistryHive _hive;

    public RegistryKey(RegistryHive hive, KeyNodeCell cell) {
        _hive = hive;
        _cell = cell;
    }

    /**
     * Gets the class name of this registry key. Class name is rarely used.
     */
    public String getClassName() {
        if (_cell.ClassNameIndex > 0) {
            return new String(_hive.rawCellData(_cell.ClassNameIndex, _cell.ClassNameLength), Charset.forName("UTF-16LE"));
        }

        return null;
    }

    /**
     * Gets the flags of this registry key.
     */
    public EnumSet<RegistryKeyFlags> getFlags() {
        return _cell.Flags;
    }

    /**
     * Gets the name of this key.
     */
    public String getName() {
        RegistryKey parent = getParent();
        if (parent != null && !parent.getFlags().contains(RegistryKeyFlags.Root)) {
            return parent.getName() + "\\" + _cell.Name;
        }
        return _cell.Name;
    }

    /**
     * Gets the parent key, or {@code null} if this is the root key.
     */
    public RegistryKey getParent() {
        if (!_cell.Flags.contains(RegistryKeyFlags.Root)) {
            return new RegistryKey(_hive, _hive.getCell(_cell.ParentIndex));
        }
        return null;
    }

    /**
     * Gets the number of child keys.
     */
    public int getSubKeyCount() {
        return _cell.NumSubKeys;
    }

    /**
     * Gets an enumerator over all sub child keys.
     */
    public List<RegistryKey> getSubKeys() {
        List<RegistryKey> result = new ArrayList<>();
        if (_cell.NumSubKeys != 0) {
            ListCell list = _hive.getCell(_cell.SubKeysIndex);
            for (KeyNodeCell key : list.enumerateKeys()) {
                result.add(new RegistryKey(_hive, key));
            }
        }
        return result;
    }

    /**
     * Gets the time the key was last modified.
     */
    public long getTimestamp() {
        return _cell.Timestamp;
    }

    /**
     * Gets the number of values in this key.
     */
    public int getValueCount() {
        return _cell.NumValues;
    }

    /**
     * Gets an enumerator over all values in this key.
     */
    private List<RegistryValue> getValues() {
        List<RegistryValue> result = new ArrayList<>();
        if (_cell.NumValues != 0) {
            byte[] valueList = _hive.rawCellData(_cell.ValueListIndex, _cell.NumValues * 4);
            for (int i = 0; i < _cell.NumValues; ++i) {
                int valueIndex = EndianUtilities.toInt32LittleEndian(valueList, i * 4);
                result.add(new RegistryValue(_hive, _hive.getCell(valueIndex)));
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
        if (_cell.SecurityIndex > 0) {
            SecurityCell secCell = _hive.getCell(_cell.SecurityIndex);
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
        if (_cell.NumSubKeys != 0) {
            ListCell cell = _hive.getCell(_cell.SubKeysIndex);
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
     * @param options Flags controlling how the value is processed before it's
     *            returned.
     * @return The value as a .NET object.
     */
    public Object getValue(String name, Object defaultValue, RegistryValueOptions options) {
        RegistryValue regVal = getRegistryValue(name);
        if (regVal != null) {
            if (regVal.getDataType() == RegistryValueType.ExpandString &&
                options != RegistryValueOptions.DoNotExpandEnvironmentNames) {
                return Utilities.expandEnvironmentVariables(String.class.cast(regVal.getValue()));
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
        if (_cell.NumValues != 0) {
            byte[] valueList = _hive.rawCellData(_cell.ValueListIndex, _cell.NumValues * 4);
            int i = 0;
            while (i < _cell.NumValues) {
                int valueIndex = EndianUtilities.toInt32LittleEndian(valueList, i * 4);
                ValueCell valueCell = _hive.getCell(valueIndex);
//Debug.println(valueCell.getName() + ", " + name);
                if (StringUtilities.compare(valueCell.getName(), name, true) == 0) {
                    foundValue = true;
                    _hive.freeCell(valueIndex);
                    _cell.NumValues--;
                    _hive.updateCell(_cell, false);
                    break;
                }

                ++i;
            }
            // Move following value's to fill gap
            if (i < _cell.NumValues) {
                while (i < _cell.NumValues) {
                    int valueIndex = EndianUtilities.toInt32LittleEndian(valueList, (i + 1) * 4);
                    EndianUtilities.writeBytesLittleEndian(valueIndex, valueList, i * 4);
                    ++i;
                }
                _hive.writeRawCellData(_cell.ValueListIndex, valueList, 0, _cell.NumValues * 4);
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

        String[] split = subkey.split(StringUtilities.escapeForRegex("\\"), 2);
        int cellIndex = findSubKeyCell(split[0]);
        if (cellIndex < 0) {
            KeyNodeCell newKeyCell = new KeyNodeCell(split[0], _cell.getIndex());
            newKeyCell.SecurityIndex = _cell.SecurityIndex;
            referenceSecurityCell(newKeyCell.SecurityIndex);
            _hive.updateCell(newKeyCell, true);
            linkSubKey(split[0], newKeyCell.getIndex());
            if (split.length == 1) {
                return new RegistryKey(_hive, newKeyCell);
            }

            return new RegistryKey(_hive, newKeyCell).createSubKey(split[1]);
        }

        KeyNodeCell cell = _hive.getCell(cellIndex);
        if (split.length == 1) {
            return new RegistryKey(_hive, cell);
        }

        return new RegistryKey(_hive, cell).createSubKey(split[1]);
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

        String[] split = path.split(StringUtilities.escapeForRegex("\\"), 2);
//Debug.println(StringUtil.paramString(split));
        int cellIndex = findSubKeyCell(split[0]);
        if (cellIndex < 0) {
            return null;
        }

        KeyNodeCell cell = _hive.getCell(cellIndex);
        if (split.length == 1) {
            return new RegistryKey(_hive, cell);
        }

        return new RegistryKey(_hive, cell).openSubKey(split[1]);
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

        String[] split = subkey.split(StringUtilities.escapeForRegex("\\"), 2);
        int subkeyCellIndex = findSubKeyCell(split[0]);
        if (subkeyCellIndex < 0) {
            if (throwOnMissingSubKey) {
                throw new IllegalArgumentException("No such SubKey");
            }

            return;
        }

        KeyNodeCell subkeyCell = _hive.getCell(subkeyCellIndex);
        if (split.length == 1) {
            if (subkeyCell.NumSubKeys != 0) {
                throw new UnsupportedOperationException("The registry key has subkeys");
            }

            if (subkeyCell.ClassNameIndex != -1) {
                _hive.freeCell(subkeyCell.ClassNameIndex);
                subkeyCell.ClassNameIndex = -1;
                subkeyCell.ClassNameLength = 0;
            }

            if (subkeyCell.SecurityIndex != -1) {
                dereferenceSecurityCell(subkeyCell.SecurityIndex);
                subkeyCell.SecurityIndex = -1;
            }

            if (subkeyCell.SubKeysIndex != -1) {
                freeSubKeys(subkeyCell);
            }

            if (subkeyCell.ValueListIndex != -1) {
                freeValues(subkeyCell);
            }

            unlinkSubKey(subkey);
            _hive.freeCell(subkeyCellIndex);
            _hive.updateCell(_cell, false);
        } else {
            new RegistryKey(_hive, subkeyCell).deleteSubKey(split[1], throwOnMissingSubKey);
        }
    }

    private RegistryValue getRegistryValue(String name) {
        if (name != null && name.length() == 0) {
            name = null;
        }

        if (_cell.NumValues != 0) {
            byte[] valueList = _hive.rawCellData(_cell.ValueListIndex, _cell.NumValues * 4);
            for (int i = 0; i < _cell.NumValues; ++i) {
                int valueIndex = EndianUtilities.toInt32LittleEndian(valueList, i * 4);
                ValueCell cell = _hive.getCell(valueIndex);
//Debug.println(name + ", " + cell);
                if (StringUtilities.compare(cell.getName(), name, true) == 0) {
                    return new RegistryValue(_hive, cell);
                }
            }
        }

        return null;
    }

    private RegistryValue addRegistryValue(String name) {
        byte[] valueList = _hive.rawCellData(_cell.ValueListIndex, _cell.NumValues * 4);
        if (valueList == null) {
            valueList = new byte[0];
        }

        int insertIdx = 0;
        while (insertIdx < _cell.NumValues) {
            int valueCellIndex = EndianUtilities.toInt32LittleEndian(valueList, insertIdx * 4);
            ValueCell cell = _hive.getCell(valueCellIndex);
            if (StringUtilities.compare(name, cell.getName(), true) < 0) {
                break;
            }

            ++insertIdx;
        }
        // Allocate a new value cell (note _hive.UpdateCell does actual
        // allocation).
        ValueCell valueCell = new ValueCell(name);
        _hive.updateCell(valueCell, true);

        // Update the value list, re-allocating if necessary
        byte[] newValueList = new byte[_cell.NumValues * 4 + 4];
        System.arraycopy(valueList, 0, newValueList, 0, insertIdx * 4);
        EndianUtilities.writeBytesLittleEndian(valueCell.getIndex(), newValueList, insertIdx * 4);
        System.arraycopy(valueList, insertIdx * 4, newValueList, insertIdx * 4 + 4, (_cell.NumValues - insertIdx) * 4);
        if (_cell.ValueListIndex == -1 || !_hive.writeRawCellData(_cell.ValueListIndex, newValueList, 0, newValueList.length)) {
            int newListCellIndex = _hive.allocateRawCell(MathUtilities.roundUp(newValueList.length, 8));
            _hive.writeRawCellData(newListCellIndex, newValueList, 0, newValueList.length);

            if (_cell.ValueListIndex != -1) {
                _hive.freeCell(_cell.ValueListIndex);
            }

            _cell.ValueListIndex = newListCellIndex;
        }

        // Record the new value and save this cell
        _cell.NumValues++;
        _hive.updateCell(_cell, false);

        // Finally, set the data in the value cell
//Debug.println(valueCell);
        return new RegistryValue(_hive, valueCell);
    }

    private int findSubKeyCell(String name) {
        if (_cell.NumSubKeys != 0) {
            ListCell listCell = _hive.getCell(_cell.SubKeysIndex);
            int cellIndex[] = new int[1];
            if (listCell.findKey(name, cellIndex) == 0) {
                return cellIndex[0];
            }
        }

        return -1;
    }

    private void linkSubKey(String name, int cellIndex) {
        if (_cell.SubKeysIndex == -1) {
            SubKeyHashedListCell newListCell = new SubKeyHashedListCell(_hive, "lf");
            newListCell.add(name, cellIndex);
            _hive.updateCell(newListCell, true);
            _cell.NumSubKeys = 1;
            _cell.SubKeysIndex = newListCell.getIndex();
        } else {
            ListCell list = _hive.getCell(_cell.SubKeysIndex);
            _cell.SubKeysIndex = list.linkSubKey(name, cellIndex);
            _cell.NumSubKeys++;
        }
        _hive.updateCell(_cell, false);
    }

    private void unlinkSubKey(String name) {
        if (_cell.SubKeysIndex == -1 || _cell.NumSubKeys == 0) {
            throw new UnsupportedOperationException("No subkey list");
        }

        ListCell list = _hive.getCell(_cell.SubKeysIndex);
        _cell.SubKeysIndex = list.unlinkSubKey(name);
        _cell.NumSubKeys--;
    }

    private void referenceSecurityCell(int cellIndex) {
        SecurityCell sc = _hive.getCell(cellIndex);
        sc.setUsageCount(sc.getUsageCount() + 1);
        _hive.updateCell(sc, false);
    }

    private void dereferenceSecurityCell(int cellIndex) {
        SecurityCell sc = _hive.getCell(cellIndex);
        sc.setUsageCount(sc.getUsageCount() - 1);
        if (sc.getUsageCount() == 0) {
            SecurityCell prev = _hive.getCell(sc.getPreviousIndex());
            prev.setNextIndex(sc.getNextIndex());
            _hive.updateCell(prev, false);

            SecurityCell next = _hive.getCell(sc.getNextIndex());
            next.setPreviousIndex(sc.getPreviousIndex());
            _hive.updateCell(next, false);

            _hive.freeCell(cellIndex);
        } else {
            _hive.updateCell(sc, false);
        }
    }

    private void freeValues(KeyNodeCell cell) {
        if (cell.NumValues != 0 && cell.ValueListIndex != -1) {
            byte[] valueList = _hive.rawCellData(cell.ValueListIndex, cell.NumValues * 4);

            for (int i = 0; i < cell.NumValues; ++i) {
                int valueIndex = EndianUtilities.toInt32LittleEndian(valueList, i * 4);
                _hive.freeCell(valueIndex);
            }

            _hive.freeCell(cell.ValueListIndex);
            cell.ValueListIndex = -1;
            cell.NumValues = 0;
            cell.MaxValDataBytes = 0;
            cell.MaxValNameBytes = 0;
        }
    }

    private void freeSubKeys(KeyNodeCell subkeyCell) {
        if (subkeyCell.SubKeysIndex == -1) {
            throw new UnsupportedOperationException("No subkey list");
        }

        Cell list = _hive.getCell(subkeyCell.SubKeysIndex);
        if (list instanceof SubKeyIndirectListCell) {
            SubKeyIndirectListCell indirectList = SubKeyIndirectListCell.class.cast(list);
            // foreach (int listIndex in indirectList.CellIndexes)
            for (int i = 0; i < indirectList.getCellIndexes().size(); ++i) {
                int listIndex = indirectList.getCellIndexes().get(i);
                _hive.freeCell(listIndex);
            }
        }

        _hive.freeCell(list.getIndex());
    }

    public String toString() {
        return _cell.toString();
    }
}
