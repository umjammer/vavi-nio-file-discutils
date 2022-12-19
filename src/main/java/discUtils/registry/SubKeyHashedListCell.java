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
import dotnet4j.util.compat.StringUtilities;
import vavi.util.ByteUtil;


public final class SubKeyHashedListCell extends ListCell {

    private String hashType;

    private final RegistryHive hive;

    private List<Integer> nameHashes;

    private short numElements;

    private List<Integer> subKeyIndexes;

    public SubKeyHashedListCell(RegistryHive hive, String hashType) {
        super(-1);
        this.hive = hive;
        this.hashType = hashType;
        subKeyIndexes = new ArrayList<>();
        nameHashes = new ArrayList<>();
    }

    public SubKeyHashedListCell(RegistryHive hive, int index) {
        super(index);
        this.hive = hive;
    }

    int getCount() {
        return subKeyIndexes.size();
    }

    public int size() {
        return 0x4 + numElements * 0x8;
    }

    public int readFrom(byte[] buffer, int offset) {
        hashType = new String(buffer, offset, 2, StandardCharsets.US_ASCII);
        numElements = ByteUtil.readLeShort(buffer, offset + 2);
        subKeyIndexes = new ArrayList<>(numElements);
        nameHashes = new ArrayList<>(numElements);
        for (int i = 0; i < numElements; ++i) {
            subKeyIndexes.add(ByteUtil.readLeInt(buffer, offset + 0x4 + i * 0x8));
            nameHashes.add(ByteUtil.readLeInt(buffer, offset + 0x4 + i * 0x8 + 0x4));
        }
        return 0x4 + numElements * 0x8;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(hashType, buffer, offset, 2);
        ByteUtil.writeLeShort(numElements, buffer, offset + 0x2);
        for (int i = 0; i < numElements; ++i) {
            ByteUtil.writeLeInt(subKeyIndexes.get(i), buffer, offset + 0x4 + i * 0x8);
            ByteUtil.writeLeInt(nameHashes.get(i), buffer, offset + 0x4 + i * 0x8 + 0x4);
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
        for (int i = 0; i < numElements; ++i) {
            KeyNodeCell cell = hive.getCell(subKeyIndexes.get(i));
            if (cell.name.compareTo(name) > 0) {
                subKeyIndexes.add(i, cellIndex);
                nameHashes.add(i, calcHash(name));
                numElements++;
                return i;
            }

        }
        subKeyIndexes.add(cellIndex);
        nameHashes.add(calcHash(name));
        return numElements++;
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

        result = findKeyAt(name, subKeyIndexes.size() - 1, found);
        cellIndex[0] = found[0];
        if (result >= 0) {
            return result;
        }

        KeyFinder finder = new KeyFinder(hive, name);
        int idx = Collections.binarySearch(subKeyIndexes, -1, finder);
        cellIndex[0] = finder.getCellIndex();
        return idx < 0 ? -1 : 0;
    }

    void enumerateKeys(List<String> names) {
        for (Integer subKeyIndex : subKeyIndexes) {
            names.add(hive.<KeyNodeCell>getCell(subKeyIndex).name);
        }
    }

    List<KeyNodeCell> enumerateKeys() {
        List<KeyNodeCell> result = new ArrayList<>();
        for (Integer subKeyIndex : subKeyIndexes) {
            result.add(hive.getCell(subKeyIndex));
        }
        return result;
    }

    int linkSubKey(String name, int cellIndex) {
        add(name, cellIndex);
        return hive.updateCell(this, true);
    }

    int unlinkSubKey(String name) {
        int index = indexOf(name);
        if (index >= 0) {
            removeAt(index);
            return hive.updateCell(this, true);
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
            KeyNodeCell cell = hive.getCell(subKeyIndexes.get(index));
            if (cell.name.equalsIgnoreCase(name)) {
                return index;
            }
        }
        return -1;
    }

    public void removeAt(int index) {
        nameHashes.remove(index);
        subKeyIndexes.remove(index);
        numElements--;
    }

    private int calcHash(String name) {
        int hash = 0;
        if (hashType.equals("lh")) {
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
        Cell cell = hive.getCell(subKeyIndexes.get(listIndex));
        if (cell == null) {
            cellIndex[0] = 0;
            return -1;
        }

        ListCell listCell = cell instanceof ListCell ? (ListCell) cell : null;
        if (listCell != null) {
            return listCell.findKey(name, cellIndex);
        }

        cellIndex[0] = subKeyIndexes.get(listIndex);
        return StringUtilities.compare(name, ((KeyNodeCell) cell).name, true);
    }

    private List<Integer> find(String name, int start) {
        if (hashType.equals("lh")) {
            return findByHash(name, start);
        }

        return findByPrefix(name, start);
    }

    private List<Integer> findByHash(String name, int start) {
        List<Integer> result = new ArrayList<>();
        int hash = calcHash(name);
        for (int i = start; i < nameHashes.size(); ++i) {
            if (nameHashes.get(i) == hash) {
                result.add(i);
            }
        }
        return result;
    }

    private List<Integer> findByPrefix(String name, int start) {
        int compChars = Math.min(name.length(), 4);
        String compStr = name.substring(0, compChars).toUpperCase() + "\0\0\0\0";
        List<Integer> result = new ArrayList<>();
        for (int i = start; i < nameHashes.size(); ++i) {
            boolean match = true;
            int hash = nameHashes.get(i);
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
            // TODO: Be more efficient at ruling out no-hopes by using the hash values
            KeyNodeCell cell = hive.getCell(x);
            int result = StringUtilities.compare(cell.name, searchName, true);
            if (result == 0) {
                cellIndex = x;
            }

            return result;
        }
    }
}
