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

package discUtils.ntfs;

/**
 * Fully-qualified reference to an attribute.
 */
public class AttributeReference implements Comparable<AttributeReference> {
    private FileRecordReference _fileReference;

    /**
     * Initializes a new instance of the AttributeReference class.
     *
     * @param fileReference The file containing the attribute.
     * @param attributeId The identity of the attribute within the file record.
     */
    public AttributeReference(FileRecordReference fileReference, short attributeId) {
        _fileReference = fileReference;
        _attributeId = attributeId;
    }

    /**
     * Gets the identity of the attribute within the file record.
     */
    private short _attributeId;

    public short getAttributeId() {
        return _attributeId;
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

        return _attributeId - other._attributeId;
    }

    /**
     * Indicates if two references are equivalent.
     *
     * @param other The attribute reference to compare.
     * @return {@code true} if the references are equivalent.
     */
    public boolean equals(AttributeReference other) {
        return compareTo(other) == 0;
    }

    /**
     * The reference as a string.
     *
     * @return String representing the attribute.
     */
    public String toString() {
        return _fileReference + ".attr[" + _attributeId + "]";
    }

    /**
     * Indicates if this reference is equivalent to another object.
     *
     * @param obj The object to compare.
     * @return {@code true} if obj is an equivalent attribute reference.
     */
    public boolean equals(Object obj) {
        AttributeReference objAsAttrRef = obj instanceof AttributeReference ? (AttributeReference) obj
                                                                            : null;
        if (objAsAttrRef == null) {
            return false;
        }

        return equals(objAsAttrRef);
    }

    /**
     * Gets the hash code for this reference.
     *
     * @return The hash code.
     */
    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(_fileReference, getAttributeId());
    }
}
