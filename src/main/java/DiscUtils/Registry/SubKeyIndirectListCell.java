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


public final class SubKeyIndirectListCell extends ListCell {
    private final RegistryHive _hive;

    public SubKeyIndirectListCell(RegistryHive hive, int index) {
        super(index);
        _hive = hive;
    }

    private List<Integer> __CellIndexes;

    public List<Integer> getCellIndexes() {
        return __CellIndexes;
    }

    public void setCellIndexes(List<Integer> value) {
        __CellIndexes = value;
    }

    public int getCount() {
        int total = 0;
        for (int cellIndex : getCellIndexes()) {
            Cell cell = _hive.getCell(cellIndex);
            ListCell listCell = cell instanceof ListCell ? (ListCell) cell : (ListCell) null;
            if (listCell != null) {
                total += listCell.getCount();
            } else {
                total++;
            }
        }
        return total;
    }

    private String __ListType;

    public String getListType() {
        return __ListType;
    }

    public void setListType(String value) {
        __ListType = value;
    }

    public int size() {
        return 4 + getCellIndexes().size() * 4;
    }

    public int readFrom(byte[] buffer, int offset) {
        setListType(EndianUtilities.bytesToString(buffer, offset, 2));
        int numElements = EndianUtilities.toInt16LittleEndian(buffer, offset + 2);
        setCellIndexes(new ArrayList<Integer>(numElements));
        for (int i = 0; i < numElements; ++i) {
            getCellIndexes().add(EndianUtilities.toInt32LittleEndian(buffer, offset + 0x4 + i * 0x4));
        }
        return 4 + getCellIndexes().size() * 4;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(getListType(), buffer, offset, 2);
        EndianUtilities.writeBytesLittleEndian((short) getCellIndexes().size(), buffer, offset + 2);
        for (int i = 0; i < getCellIndexes().size(); ++i) {
            EndianUtilities.writeBytesLittleEndian(getCellIndexes().get(i), buffer, offset + 4 + i * 4);
        }
    }

    public int findKey(String name, int[] cellIndex) {
        if (getCellIndexes().size() <= 0) {
            cellIndex[0] = 0;
            return -1;
        }

        // Check first and last, to early abort if the name is outside the range of this list
        int found[] = new int[1];
        int result = doFindKey(name, 0, found);
        cellIndex[0] = found[0];
        if (result <= 0) {
            return result;
        }

        result = doFindKey(name, getCellIndexes().size() - 1, found);
        cellIndex[0] = found[0];
        if (result >= 0) {
            return result;
        }

        KeyFinder finder = new KeyFinder(_hive, name);
        int idx = Collections.binarySearch(getCellIndexes(), null, finder); // , -1 reverse ???
        cellIndex[0] = finder.getCellIndex();
        return idx < 0 ? -1 : 0;
    }

    public void enumerateKeys(List<String> names) {
        for (int i = 0; i < getCellIndexes().size(); ++i) {
            Cell cell = _hive.<Cell> getCell(getCellIndexes().get(i));
            ListCell listCell = cell instanceof ListCell ? (ListCell) cell : (ListCell) null;
            if (listCell != null) {
                listCell.enumerateKeys(names);
            } else {
                names.add(((KeyNodeCell) cell).Name);
            }
        }
    }

    public List<KeyNodeCell> enumerateKeys() {
        List<KeyNodeCell> result = new ArrayList<>();
        for (int i = 0; i < getCellIndexes().size(); ++i) {
            Cell cell = _hive.<Cell> getCell(getCellIndexes().get(i));
            ListCell listCell = cell instanceof ListCell ? (ListCell) cell : (ListCell) null;
            if (listCell != null) {
                for (KeyNodeCell keyNodeCell : listCell.enumerateKeys()) {
                    result.add(keyNodeCell);
                }
            } else {
                result.add(null);
            }
        }
        return result;
    }

    public int linkSubKey(String name, int cellIndex) {
        // Look for the first sublist that has a subkey name greater than name
        if (getListType().equals("ri")) {
            if (getCellIndexes().size() == 0) {
                throw new UnsupportedOperationException("Empty indirect list");
            }

            for (int i = 0; i < getCellIndexes().size() - 1; ++i) {
                ListCell cell = _hive.<ListCell> getCell(getCellIndexes().get(i));
                int[] tempIndex = new int[1];
                if (cell.findKey(name, tempIndex) <= 0) {
                    getCellIndexes().set(i, cell.linkSubKey(name, cellIndex));
                    return _hive.updateCell(this, false);
                }

            }
            ListCell lastCell = _hive.<ListCell> getCell(getCellIndexes().get(getCellIndexes().size() - 1));
            getCellIndexes().set(getCellIndexes().size() - 1, lastCell.linkSubKey(name, cellIndex));
            return _hive.updateCell(this, false);
        }

        for (int i = 0; i < getCellIndexes().size(); ++i) {
            KeyNodeCell cell = _hive.<KeyNodeCell> getCell(getCellIndexes().get(i));
            if (name.compareTo(cell.Name) < 0) {
                getCellIndexes().add(i, cellIndex);
                return _hive.updateCell(this, true);
            }

        }
        getCellIndexes().add(cellIndex);
        return _hive.updateCell(this, true);
    }

    public int unlinkSubKey(String name) {
        if (getListType().equals("ri")) {
            if (getCellIndexes().size() == 0) {
                throw new UnsupportedOperationException("Empty indirect list");
            }

            for (int i = 0; i < getCellIndexes().size(); ++i) {
                ListCell cell = _hive.<ListCell> getCell(getCellIndexes().get(i));
                int[] tempIndex = new int[1];
                boolean result = cell.findKey(name, tempIndex) <= 0;
                if (result) {
                    getCellIndexes().set(i, cell.unlinkSubKey(name));
                    if (cell.getCount() == 0) {
                        _hive.freeCell(getCellIndexes().get(i));
                        getCellIndexes().remove(i);
                    }
                    return _hive.updateCell(this, false);
                }
            }
        } else {
            for (int i = 0; i < getCellIndexes().size(); ++i) {
                KeyNodeCell cell = _hive.<KeyNodeCell> getCell(getCellIndexes().get(i));
                if (name.compareTo(cell.Name) == 0) {
                    getCellIndexes().remove(i);
                    return _hive.updateCell(this, true);
                }
            }
        }
        return getIndex();
    }

    private int doFindKey(String name, int listIndex, int[] cellIndex) {
        Cell cell = _hive.<Cell> getCell(getCellIndexes().get(listIndex));
        ListCell listCell = cell instanceof ListCell ? (ListCell) cell : (ListCell) null;
        if (listCell != null) {
            int[] found = new int[1];
            int result = listCell.findKey(name, found);
            cellIndex[0] = found[0];
            return result;
        }

        cellIndex[0] = getCellIndexes().get(listIndex);
        return name.compareTo(((KeyNodeCell) cell).Name);
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
            Cell cell = _hive.getCell(x);
            ListCell listCell = cell instanceof ListCell ? (ListCell) cell : (ListCell) null;
            int result;
            if (listCell != null) {
                int[] cellIndex = new int[1];
                result = listCell.findKey(_searchName, cellIndex);
                if (result == 0) {
                    setCellIndex(cellIndex[0]);
                }

                return -result;
            }

            result = ((KeyNodeCell) cell).Name.compareTo(_searchName);
            if (result == 0) {
                setCellIndex(x);
            }

            return result;
        }
    }
}
