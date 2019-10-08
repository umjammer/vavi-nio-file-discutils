//
// Copyright (c) 2017, Quamotion
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

package DiscUtils.Nfs;

import DiscUtils.Core.Internal.Utilities;

public class Nfs3PathConfResult extends Nfs3CallResult {
    public Nfs3PathConfResult() {
    }

    public Nfs3PathConfResult(XdrDataReader reader) {
        setStatus(Nfs3Status.valueOf(reader.readInt32()));
        setObjectAttributes(new Nfs3FileAttributes(reader));
        if (getStatus() == Nfs3Status.Ok) {
            setLinkMax(reader.readUInt32());
            setNameMax(reader.readUInt32());
            setNoTrunc(reader.readBool());
            setChownRestricted(reader.readBool());
            setCaseInsensitive(reader.readBool());
            setCasePreserving(reader.readBool());
        }
    }

    /**
     * Gets or sets the attributes of the object specified by object.
     */
    private Nfs3FileAttributes __ObjectAttributes;

    public Nfs3FileAttributes getObjectAttributes() {
        return __ObjectAttributes;
    }

    public void setObjectAttributes(Nfs3FileAttributes value) {
        __ObjectAttributes = value;
    }

    /**
     * Gets or sets the maximum number of hard links to an object.
     */
    private int __LinkMax;

    public int getLinkMax() {
        return __LinkMax;
    }

    public void setLinkMax(int value) {
        __LinkMax = value;
    }

    /**
     * Gets or sets the maximum length of a component of a filename.
     */
    private int __NameMax;

    public int getNameMax() {
        return __NameMax;
    }

    public void setNameMax(int value) {
        __NameMax = value;
    }

    /**
     * Gets or sets a value indicating whether the server will reject any
     * request that
     * includes a name longer than name_max with the error,
     * NFS3ERR_NAMETOOLONG.If FALSE, any length name over
     * name_max bytes will be silently truncated to name_max
     * bytes.
     */
    private boolean __NoTrunc;

    public boolean getNoTrunc() {
        return __NoTrunc;
    }

    public void setNoTrunc(boolean value) {
        __NoTrunc = value;
    }

    /**
     * Gets or sets a value indicating whether server will reject any request to
     * change
     * either the owner or the group associated with a file if
     * the caller is not the privileged user. (Uid 0.)
     */
    private boolean __ChownRestricted;

    public boolean getChownRestricted() {
        return __ChownRestricted;
    }

    public void setChownRestricted(boolean value) {
        __ChownRestricted = value;
    }

    /**
     * Gets or sets a value indicating whether the server file system does not
     * distinguish
     * case when interpreting filenames.
     */
    private boolean __CaseInsensitive;

    public boolean getCaseInsensitive() {
        return __CaseInsensitive;
    }

    public void setCaseInsensitive(boolean value) {
        __CaseInsensitive = value;
    }

    /**
     * Gets or sets a value indicating whether the server file system will
     * preserve the case
     * of a name during a CREATE, MKDIR, MKNOD, SYMLINK,
     * RENAME, or LINK operation.
     */
    private boolean __CasePreserving;

    public boolean getCasePreserving() {
        return __CasePreserving;
    }

    public void setCasePreserving(boolean value) {
        __CasePreserving = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(getStatus().ordinal());
        getObjectAttributes().write(writer);
        if (getStatus() == Nfs3Status.Ok) {
            writer.write(getLinkMax());
            writer.write(getNameMax());
            writer.write(getNoTrunc());
            writer.write(getChownRestricted());
            writer.write(getCaseInsensitive());
            writer.write(getCasePreserving());
        }

    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3PathConfResult ? (Nfs3PathConfResult) obj : (Nfs3PathConfResult) null);
    }

    public boolean equals(Nfs3PathConfResult other) {
        if (other == null) {
            return false;
        }

        return other.getStatus() == getStatus() && other.getLinkMax() == getLinkMax() && other.getNameMax() == getNameMax() &&
               other.getNoTrunc() == getNoTrunc() && other.getChownRestricted() == getChownRestricted() &&
               other.getCaseInsensitive() == getCaseInsensitive() && other.getCasePreserving() == getCasePreserving();
    }

    public int hashCode() {
        return Utilities.getCombinedHashCode(getStatus(),
                                getLinkMax(),
                                getNameMax(),
                                getNoTrunc(),
                                getChownRestricted(),
                                getCaseInsensitive(),
                                getCasePreserving());
    }
}
