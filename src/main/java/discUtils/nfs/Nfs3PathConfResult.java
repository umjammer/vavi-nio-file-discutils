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

package discUtils.nfs;

public class Nfs3PathConfResult extends Nfs3CallResult {

    public Nfs3PathConfResult() {
    }

    public Nfs3PathConfResult(XdrDataReader reader) {
        status = Nfs3Status.valueOf(reader.readInt32());
        objectAttributes = new Nfs3FileAttributes(reader);
        if (status == Nfs3Status.Ok) {
            linkMax = reader.readUInt32();
            nameMax = reader.readUInt32();
            noTrunc = reader.readBool();
            chownRestricted = reader.readBool();
            caseInsensitive = reader.readBool();
            casePreserving = reader.readBool();
        }
    }

    /**
     * Gets or sets the attributes of the object specified by object.
     */
    private Nfs3FileAttributes objectAttributes;

    public Nfs3FileAttributes getObjectAttributes() {
        return objectAttributes;
    }

    public void setObjectAttributes(Nfs3FileAttributes value) {
        objectAttributes = value;
    }

    /**
     * Gets or sets the maximum number of hard links to an object.
     */
    private int linkMax;

    public int getLinkMax() {
        return linkMax;
    }

    public void setLinkMax(int value) {
        linkMax = value;
    }

    /**
     * Gets or sets the maximum length of a component of a filename.
     */
    private int nameMax;

    public int getNameMax() {
        return nameMax;
    }

    public void setNameMax(int value) {
        nameMax = value;
    }

    /**
     * Gets or sets a value indicating whether the server will reject any
     * request that includes a name longer than name_max with the error,
     * NFS3ERR_NAMETOOLONG.If FALSE, any length name over name_max bytes will be
     * silently truncated to name_max bytes.
     */
    private boolean noTrunc;

    public boolean getNoTrunc() {
        return noTrunc;
    }

    public void setNoTrunc(boolean value) {
        noTrunc = value;
    }

    /**
     * Gets or sets a value indicating whether server will reject any request to
     * change either the owner or the group associated with a file if the caller
     * is not the privileged user. (Uid 0.)
     */
    private boolean chownRestricted;

    public boolean getChownRestricted() {
        return chownRestricted;
    }

    public void setChownRestricted(boolean value) {
        chownRestricted = value;
    }

    /**
     * Gets or sets a value indicating whether the server file system does not
     * distinguish case when interpreting filenames.
     */
    private boolean caseInsensitive;

    public boolean getCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(boolean value) {
        caseInsensitive = value;
    }

    /**
     * Gets or sets a value indicating whether the server file system will
     * preserve the case of a name during a CREATE, MKDIR, MKNOD, SYMLINK,
     * RENAME, or LINK operation.
     */
    private boolean casePreserving;

    public boolean getCasePreserving() {
        return casePreserving;
    }

    public void setCasePreserving(boolean value) {
        casePreserving = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(status.getValue());
        objectAttributes.write(writer);
        if (status == Nfs3Status.Ok) {
            writer.write(linkMax);
            writer.write(nameMax);
            writer.write(noTrunc);
            writer.write(chownRestricted);
            writer.write(caseInsensitive);
            writer.write(casePreserving);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3PathConfResult ? (Nfs3PathConfResult) obj : null);
    }

    public boolean equals(Nfs3PathConfResult other) {
        if (other == null) {
            return false;
        }

        return other.status == status && other.linkMax == linkMax && other.nameMax == nameMax &&
               other.noTrunc == noTrunc && other.chownRestricted == chownRestricted &&
               other.caseInsensitive == caseInsensitive && other.casePreserving == casePreserving;
    }

    public int hashCode() {
        return dotnet4j.util.compat.Utilities.getCombinedHashCode(status,
                linkMax,
                nameMax,
                noTrunc,
                chownRestricted,
                caseInsensitive,
                casePreserving);
    }
}
