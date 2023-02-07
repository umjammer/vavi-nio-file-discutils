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

import java.util.List;

public abstract class ListCell extends Cell {

    public ListCell(int index) {
        super(index);
    }

    /**
     * Gets the number of subkeys in this list.
     */
    abstract int getCount();

    /**
     * Searches for a key with a given name.
     *
     * @param name The name to search for.
     * @param cellIndex The index of the cell, if found.
     * @return The search result.
     */
    abstract int findKey(String name, int[] cellIndex);

    /**
     * Enumerates all of the keys in the list.
     *
     * @param names The list to populate.
     */
    abstract void enumerateKeys(List<String> names);

    /**
     * Enumerates all of the keys in the list.
     *
     * @return Enumeration of key cells.
     */
    abstract List<KeyNodeCell> enumerateKeys();

    /**
     * Adds a subkey to this list.
     *
     * @param name The name of the subkey.
     * @param cellIndex The cell index of the subkey.
     * @return The new cell index of the list, which may have changed.
     */
    abstract int linkSubKey(String name, int cellIndex);

    /**
     * Removes a subkey from this list.
     *
     * @param name The name of the subkey.
     * @return The new cell index of the list, which may have changed.
     */
    abstract int unlinkSubKey(String name);
}
