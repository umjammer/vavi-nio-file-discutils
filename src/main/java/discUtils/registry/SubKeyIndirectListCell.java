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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


final class SubKeyIndirectListCell extends ListCell {
    private final RegistryHive hive;

    public SubKeyIndirectListCell(RegistryHive hive, int index) {
        super(index);
        this.hive = hive;
    }

    private List<Integer> cellIndexes;

    public List<Integer> getCellIndexes() {
        return cellIndexes;
    }

    public void setCellIndexes(List<Integer> value) {
        cellIndexes = value;
    }

    int getCount() {
        int total = 0;
        for (int cellIndex : getCellIndexes()) {
            Cell cell = hive.getCell(cellIndex);
            ListCell listCell = cell instanceof ListCell ? (ListCell) cell : null;
            if (listCell != null) {
                total += listCell.getCount();
            } else {
                total++;
            }
        }
        return total;
    }

    private String listType;

    public String getListType() {
        return listType;
    }

    public void setListType(String value) {
        listType = value;
    }

    public int size() {
        return 4 + cellIndexes.size() * 4;
    }

    public int readFrom(byte[] buffer, int offset) {
        listType = new String(buffer, offset, 2, StandardCharsets.US_ASCII);
        int numElements = ByteUtil.readLeShort(buffer, offset + 2);
        cellIndexes = new ArrayList<>(numElements);
        for (int i = 0; i < numElements; ++i) {
            cellIndexes.add(ByteUtil.readLeInt(buffer, offset + 0x4 + i * 0x4));
        }
        return 4 + cellIndexes.size() * 4;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(listType, buffer, offset, 2);
        ByteUtil.writeLeShort((short) cellIndexes.size(), buffer, offset + 2);
        for (int i = 0; i < cellIndexes.size(); ++i) {
            ByteUtil.writeLeInt(cellIndexes.get(i), buffer, offset + 4 + i * 4);
        }
    }

    /**
     * @param cellIndex {@cs out}
     */
    int findKey(String name, int[] cellIndex) {
        if (cellIndexes.size() <= 0) {
            cellIndex[0] = 0;
            return -1;
        }

        // Check first and last, to early abort if the name is outside the range of this list
        int[] found = new int[1];
        int result = doFindKey(name, 0, found);
        cellIndex[0] = found[0];
        if (result <= 0) {
            return result;
        }

        result = doFindKey(name, cellIndexes.size() - 1, found);
        cellIndex[0] = found[0];
        if (result >= 0) {
            return result;
        }

        KeyFinder finder = new KeyFinder(hive, name);
        int idx = Collections.binarySearch(cellIndexes, -1, finder);
        cellIndex[0] = finder.getCellIndex();
        return idx < 0 ? -1 : 0;
    }

    void enumerateKeys(List<String> names) {
        for (Integer cellIndex : cellIndexes) {
            Cell cell = hive.getCell(cellIndex);
            ListCell listCell = cell instanceof ListCell ? (ListCell) cell : null;
            if (listCell != null) {
                listCell.enumerateKeys(names);
            } else {
                names.add(((KeyNodeCell) cell).name);
            }
        }
    }

    List<KeyNodeCell> enumerateKeys() {
        List<KeyNodeCell> result = new ArrayList<>();
        for (Integer cellIndex : cellIndexes) {
            Cell cell = hive.getCell(cellIndex);
            ListCell listCell = cell instanceof ListCell ? (ListCell) cell : null;
            if (listCell != null) {
                result.addAll(listCell.enumerateKeys());
            } else {
                result.add(null);
            }
        }
        return result;
    }

    int linkSubKey(String name, int cellIndex) {
        // Look for the first sublist that has a subkey name greater than name
        if (listType.equals("ri")) {
            if (cellIndexes.size() == 0) {
                throw new UnsupportedOperationException("Empty indirect list");
            }

            for (int i = 0; i < cellIndexes.size() - 1; ++i) {
                ListCell cell = hive.getCell(cellIndexes.get(i));
                int[] tempIndex = new int[1];
                if (cell.findKey(name, tempIndex) <= 0) {
                    cellIndexes.set(i, cell.linkSubKey(name, cellIndex));
                    return hive.updateCell(this, false);
                }

            }
            ListCell lastCell = hive.getCell(cellIndexes.get(cellIndexes.size() - 1));
            cellIndexes.set(cellIndexes.size() - 1, lastCell.linkSubKey(name, cellIndex));
            return hive.updateCell(this, false);
        }

        for (int i = 0; i < cellIndexes.size(); ++i) {
            KeyNodeCell cell = hive.
                    getCell(cellIndexes.get(i));
            if (name.compareTo(cell.name) < 0) {
                cellIndexes.add(i, cellIndex);
                return hive.updateCell(this, true);
            }

        }
        cellIndexes.add(cellIndex);
        return hive.updateCell(this, true);
    }

    int unlinkSubKey(String name) {
        if (listType.equals("ri")) {
            if (cellIndexes.size() == 0) {
                throw new UnsupportedOperationException("Empty indirect list");
            }

            for (int i = 0; i < cellIndexes.size(); ++i) {
                ListCell cell = hive.getCell(cellIndexes.get(i));
                int[] tempIndex = new int[1];
                boolean result = cell.findKey(name, tempIndex) <= 0;
                if (result) {
                    cellIndexes.set(i, cell.unlinkSubKey(name));
                    if (cell.getCount() == 0) {
                        hive.freeCell(cellIndexes.get(i));
                        cellIndexes.remove(i);
                    }
                    return hive.updateCell(this, false);
                }
            }
        } else {
            for (int i = 0; i < cellIndexes.size(); ++i) {
                KeyNodeCell cell = hive.getCell(cellIndexes.get(i));
                if (name.compareTo(cell.name) == 0) {
                    cellIndexes.remove(i);
                    return hive.updateCell(this, true);
                }
            }
        }
        return getIndex();
    }

    /**
     * @param cellIndex {@cs out}
     */
    private int doFindKey(String name, int listIndex, int[] cellIndex) {
        Cell cell = hive.getCell(cellIndexes.get(listIndex));
        ListCell listCell = cell instanceof ListCell ? (ListCell) cell : null;
        if (listCell != null) {
            int[] found = new int[1];
            int result = listCell.findKey(name, found);
            cellIndex[0] = found[0];
            return result;
        }

        cellIndex[0] = cellIndexes.get(listIndex);
        return name.compareTo(((KeyNodeCell) cell).name);
    }

    private static class KeyFinder implements Comparator<Integer> {
        private final RegistryHive hive;

        private final String searchName;

        public KeyFinder(RegistryHive hive, String searchName) {
            this.hive = hive;
            this.searchName = searchName;
        }

        private int cellIndex;

        public int getCellIndex() {
            return cellIndex;
        }

        public void setCellIndex(int value) {
            cellIndex = value;
        }

        public int compare(Integer x, Integer y) {
            Cell cell = hive.getCell(x);
            ListCell listCell = cell instanceof ListCell ? (ListCell) cell : null;
            int result;
            if (listCell != null) {
                int[] cellIndex = new int[1];
                result = listCell.findKey(searchName, cellIndex);
                if (result == 0) {
                    this.cellIndex = cellIndex[0];
                }

                return -result;
            }

            result = ((KeyNodeCell) cell).name.compareTo(searchName);
            if (result == 0) {
                cellIndex = x;
            }

            return result;
        }
    }
}
