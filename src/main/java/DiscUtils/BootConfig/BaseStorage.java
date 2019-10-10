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

package DiscUtils.BootConfig;

import java.util.List;
import java.util.UUID;

/**
 * Base class for BCD storage repositories.
 */
public abstract class BaseStorage {

    /**
     * Tests if an element is present (i.e. has a value) on an object.
     * 
     * @param obj The object to inspect.
     * @param element The element to inspect.
     * @return
     *         {@code true}
     *         if present, else
     *         {@code false}
     *         .
     */
    public abstract boolean hasValue(UUID obj, int element);

    /**
     * Gets the value of a string element.
     * 
     * @param obj The object to inspect.
     * @param element The element to retrieve.
     * @return The value as a string.
     */
    public abstract String getString(UUID obj, int element);

    public abstract byte[] getBinary(UUID obj, int element);

    public abstract void setString(UUID obj, int element, String value);

    public abstract void setBinary(UUID obj, int element, byte[] value);

    public abstract String[] getMultiString(UUID obj, int element);

    public abstract void setMultiString(UUID obj, int element, String[] values);

    public abstract List<UUID> enumerateObjects();

    public abstract List<Integer> enumerateElements(UUID obj);

    public abstract int getObjectType(UUID obj);

    public abstract boolean objectExists(UUID obj);

    public abstract UUID createObject(UUID obj, int type);

    public abstract void createElement(UUID obj, int element);

    public abstract void deleteObject(UUID obj);

    public abstract void deleteElement(UUID obj, int element);
}
