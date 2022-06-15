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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import DiscUtils.Streams.Util.EndianUtilities;
import dotnet4j.io.compat.StringUtilities;


public final class SubKeyHashedListCell extends ListCell {
    private String _hashType;

    private final RegistryHive _hive;

    private List<Integer> _nameHashes;

    private short _numElements;

    private List<Integer> _subKeyIndexes;

    public SubKeyHashedListCell(RegistryHive hive, String hashType) {
        super(-1);
        _hive = hive;
        _hashType = hashType;
        _subKeyIndexes = new ArrayList<>();
        _nameHashes = new ArrayList<>();
    }

    public SubKeyHashedListCell(RegistryHive hive, int index) {
        super(index);
        _hive = hive;
    }

    int getCount() {
        return _subKeyIndexes.size();
    }

    public int size() {
        return 0x4 + _numElements * 0x8;
    }

    public int readFrom(byte[] buffer, int offset) {
        _hashType = EndianUtilities.bytesToString(buffer, offset, 2);
        _numElements = EndianUtilities.toInt16LittleEndian(buffer, offset + 2);
        _subKeyIndexes = new ArrayList<>(_numElements);
        _nameHashes = new ArrayList<>(_numElements);
        for (int i = 0; i < _numElements; ++i) {
            _subKeyIndexes.add(EndianUtilities.toInt32LittleEndian(buffer, offset + 0x4 + i * 0x8));
            _nameHashes.add(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x4 + i * 0x8 + 0x4));
        }
        return 0x4 + _numElements * 0x8;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(_hashType, buffer, offset, 2);
        EndianUtilities.writeBytesLittleEndian(_numElements, buffer, offset + 0x2);
        for (int i = 0; i < _numElements; ++i) {
            EndianUtilities.writeBytesLittleEndian(_subKeyIndexes.get(i), buffer, offset + 0x4 + i * 0x8);
            EndianUtilities.writeBytesLittleEndian(_nameHashes.get(i), buffer, offset + 0x4 + i * 0x8 + 0x4);
        }
    }

    /**
     * Adds a new entry.
     *
     * @param name The name of the subkey.
     * @param cellIndex The cell index of the subkey.
     * @return The index of the new entry.
     */
    public int add(String name, int cellIndex) {
        for (int i = 0; i < _numElements; ++i) {
            KeyNodeCell cell = _hive.getCell(_subKeyIndexes.get(i));
            if (cell.Name.compareTo(name) > 0) {
                _subKeyIndexes.add(i, cellIndex);
                _nameHashes.add(i, calcHash(name));
                _numElements++;
                return i;
            }

        }
        _subKeyIndexes.add(cellIndex);
        _nameHashes.add(calcHash(name));
        return _numElements++;
    }

    /**
     * @param cellIndex {@cs out}
     */
    int findKey(String name, int[] cellIndex) {
        // Check first and last, to early abort if the name is outside the range of this list
        int[] found = new int[1];
        int result = findKeyAt(name, 0, found);
        cellIndex[0] = found[0];
        if (result <= 0) {
            return result;
        }

        result = findKeyAt(name, _subKeyIndexes.size() - 1, found);
        cellIndex[0] = found[0];
        if (result >= 0) {
            return result;
        }

        KeyFinder finder = new KeyFinder(_hive, name);
        int idx = Collections.binarySearch(_subKeyIndexes, -1, finder);
        cellIndex[0] = finder.getCellIndex();
        return idx < 0 ? -1 : 0;
    }

    void enumerateKeys(List<String> names) {
        for (Integer subKeyIndex : _subKeyIndexes) {
            names.add(_hive.<KeyNodeCell>getCell(subKeyIndex).Name);
        }
    }

    List<KeyNodeCell> enumerateKeys() {
        List<KeyNodeCell> result = new ArrayList<>();
        for (Integer subKeyIndex : _subKeyIndexes) {
            result.add(_hive.getCell(subKeyIndex));
        }
        return result;
    }

    int linkSubKey(String name, int cellIndex) {
        add(name, cellIndex);
        return _hive.updateCell(this, true);
    }

    int unlinkSubKey(String name) {
        int index = indexOf(name);
        if (index >= 0) {
            removeAt(index);
            return _hive.updateCell(this, true);
        }

        return getIndex();
    }

    /**
     * Finds a subkey cell, returning it's index in this list.
     *
     * @param name The name of the key to find.
     * @return The index of the found key, or
     *         {@code -1}
     *         .
     */
    public int indexOf(String name) {
        for (int index : find(name, 0)) {
            KeyNodeCell cell = _hive.getCell(_subKeyIndexes.get(index));
            if (cell.Name.equalsIgnoreCase(name)) {
                return index;
            }
        }
        return -1;
    }

    public void removeAt(int index) {
        _nameHashes.remove(index);
        _subKeyIndexes.remove(index);
        _numElements--;
    }

    private int calcHash(String name) {
        int hash = 0;
        if (_hashType.equals("lh")) {
            for (int i = 0; i < name.length(); ++i) {
                hash *= 37;
                hash += Character.toUpperCase(name.charAt(i));
            }
        } else {
            String hashStr = name + "\0\0\0\0";
            for (int i = 0; i < 4; ++i) {
                hash |= (hashStr.charAt(i) & 0xFF) << (i * 8);
            }
        }
        return hash;
    }

    /**
     * @param cellIndex {@cs out}
     */
    private int findKeyAt(String name, int listIndex, int[] cellIndex) {
        Cell cell = _hive.getCell(_subKeyIndexes.get(listIndex));
        if (cell == null) {
            cellIndex[0] = 0;
            return -1;
        }

        ListCell listCell = cell instanceof ListCell ? (ListCell) cell : null;
        if (listCell != null) {
            return listCell.findKey(name, cellIndex);
        }

        cellIndex[0] = _subKeyIndexes.get(listIndex);
        return StringUtilities.compare(name, ((KeyNodeCell) cell).Name, true);
    }

    private List<Integer> find(String name, int start) {
        if (_hashType.equals("lh")) {
            return findByHash(name, start);
        }

        return findByPrefix(name, start);
    }

    private List<Integer> findByHash(String name, int start) {
        List<Integer> result = new ArrayList<>();
        int hash = calcHash(name);
        for (int i = start; i < _nameHashes.size(); ++i) {
            if (_nameHashes.get(i) == hash) {
                result.add(i);
            }
        }
        return result;
    }

    private List<Integer> findByPrefix(String name, int start) {
        int compChars = Math.min(name.length(), 4);
        String compStr = name.substring(0, compChars).toUpperCase() + "\0\0\0\0";
        List<Integer> result = new ArrayList<>();
        for (int i = start; i < _nameHashes.size(); ++i) {
            boolean match = true;
            int hash = _nameHashes.get(i);
            for (int j = 0; j < 4; ++j) {
                char ch = (char) ((hash >>> (j * 8)) & 0xFF);
                if (Character.toUpperCase(ch) != compStr.charAt(j)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                result.add(i);
            }
        }
        return result;
    }

    private static class KeyFinder implements Comparator<Integer> {
        private final RegistryHive _hive;

        private final String _searchName;

        public KeyFinder(RegistryHive hive, String searchName) {
            _hive = hive;
            _searchName = searchName;
        }

        private int __CellIndex;

        public int getCellIndex() {
            return __CellIndex;
        }

        public void setCellIndex(int value) {
            __CellIndex = value;
        }

        public int compare(Integer x, Integer y) {
            // TODO: Be more efficient at ruling out no-hopes by using the hash values
            KeyNodeCell cell = _hive.getCell(x);
            int result = StringUtilities.compare(cell.Name, _searchName, true);
            if (result == 0) {
                setCellIndex(x);
            }

            return result;
        }
    }
}
