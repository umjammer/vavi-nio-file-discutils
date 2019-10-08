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

package DiscUtils.Ntfs;

import DiscUtils.Core.Internal.Utilities;

/**
 * Fully-qualified reference to an attribute.
 */
public class AttributeReference implements Comparable<AttributeReference> {
    private FileRecordReference _fileReference = new FileRecordReference();

    /**
     * Initializes a new instance of the AttributeReference class.
     *
     * @param fileReference The file containing the attribute.
     * @param attributeId The identity of the attribute within the file record.
     */
    public AttributeReference(FileRecordReference fileReference, short attributeId) {
        _fileReference = fileReference;
        __AttributeId = attributeId;
    }

    /**
     * Gets the identity of the attribute within the file record.
     */
    private short __AttributeId;

    public short getAttributeId() {
        return __AttributeId;
    }

    /**
     * Gets the file containing the attribute.
     */
    public FileRecordReference getFile() {
        return _fileReference;
    }

    /**
     * Compares this attribute reference to another.
     *
     * @param other The attribute reference to compare against.
     * @return Zero if references are identical.
     */
    public int compareTo(AttributeReference other) {
        int refDiff = _fileReference.compareTo(other._fileReference);
        if (refDiff != 0) {
            return refDiff;
        }

        return getAttributeId() - other.getAttributeId();
    }

    /**
     * Indicates if two references are equivalent.
     *
     * @param other The attribute reference to compare.
     * @return
     *         {@code true}
     *         if the references are equivalent.
     */
    public boolean equals(AttributeReference other) {
        try {
            return compareTo(other) == 0;
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    /**
     * The reference as a string.
     *
     * @return String representing the attribute.
     */
    public String toString() {
        try {
            return _fileReference + ".attr[" + getAttributeId() + "]";
        } catch (RuntimeException __dummyCatchVar1) {
            throw __dummyCatchVar1;
        } catch (Exception __dummyCatchVar1) {
            throw new RuntimeException(__dummyCatchVar1);
        }

    }

    /**
     * Indicates if this reference is equivalent to another object.
     *
     * @param obj The object to compare.
     * @return
     *         {@code true}
     *         if obj is an equivalent attribute reference.
     */
    public boolean equals(Object obj) {
        try {
            AttributeReference objAsAttrRef = obj instanceof AttributeReference ? (AttributeReference) obj
                                                                                : (AttributeReference) null;
            if (objAsAttrRef == null) {
                return false;
            }

            return equals(objAsAttrRef);
        } catch (RuntimeException __dummyCatchVar2) {
            throw __dummyCatchVar2;
        } catch (Exception __dummyCatchVar2) {
            throw new RuntimeException(__dummyCatchVar2);
        }

    }

    /**
     * Gets the hash code for this reference.
     *
     * @return The hash code.
     */
    public int hashCode() {
        return Utilities.getCombinedHashCode(_fileReference, getAttributeId());
    }
}
