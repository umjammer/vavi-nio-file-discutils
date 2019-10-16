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

package DiscUtils.Ntfs.Internals;

import DiscUtils.Ntfs.FileRecordReference;


/**
 * A reference to a Master File Table entry.
 */
public class MasterFileTableReference {
    public MasterFileTableReference() {
    }

    private FileRecordReference _ref;

    public MasterFileTableReference(FileRecordReference recordRef) {
        _ref = recordRef;
    }

    /**
     * Gets the index of the referred entry in the Master File Table.
     */
    public long getRecordIndex() {
        return _ref.getMftIndex();
    }

    /**
     * Gets the revision number of the entry.
     *
     * This value prevents accidental reference to an entry - it will get out
     * of sync with the actual entry if the entry is re-allocated or
     * de-allocated.
     */
    public int getRecordSequenceNumber() {
        return _ref.getSequenceNumber();
    }

    /**
     * Compares to instances for equality.
     *
     * @param a The first instance to compare.
     * @param b The second instance to compare.
     * @return
     *         {@code true}
     *         if the instances are equivalent, else
     *         {@code false}
     *         .
     */

    /**
     * Compares to instances for equality.
     *
     * @param a The first instance to compare.
     * @param b The second instance to compare.
     * @return
     *         {@code true}
     *         if the instances are not equivalent, else
     *         {@code false}
     *         .
     */

    /**
     * Compares another object for equality.
     *
     * @param obj The object to compare.
     * @return
     *         {@code true}
     *         if the other object is equivalent, else
     *         {@code false}
     *         .
     */
    public boolean equals(Object obj) {
        try {
            if (obj == null || !(obj instanceof MasterFileTableReference)) {
                return false;
            }

            return _ref == ((MasterFileTableReference) obj)._ref;
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    /**
     * Gets a hash code for this instance.
     *
     * @return The hash code.
     */
    public int hashCode() {
        return _ref.hashCode();
    }
}
