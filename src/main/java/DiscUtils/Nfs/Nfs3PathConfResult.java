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

public class Nfs3PathConfResult extends Nfs3CallResult {
    public Nfs3PathConfResult() {
    }

    public Nfs3PathConfResult(XdrDataReader reader) {
        _status = Nfs3Status.valueOf(reader.readInt32());
        _objectAttributes = new Nfs3FileAttributes(reader);
        if (_status == Nfs3Status.Ok) {
            _linkMax = reader.readUInt32();
            _nameMax = reader.readUInt32();
            _noTrunc = reader.readBool();
            _chownRestricted = reader.readBool();
            _caseInsensitive = reader.readBool();
            _casePreserving = reader.readBool();
        }
    }

    /**
     * Gets or sets the attributes of the object specified by object.
     */
    private Nfs3FileAttributes _objectAttributes;

    public Nfs3FileAttributes getObjectAttributes() {
        return _objectAttributes;
    }

    public void setObjectAttributes(Nfs3FileAttributes value) {
        _objectAttributes = value;
    }

    /**
     * Gets or sets the maximum number of hard links to an object.
     */
    private int _linkMax;

    public int getLinkMax() {
        return _linkMax;
    }

    public void setLinkMax(int value) {
        _linkMax = value;
    }

    /**
     * Gets or sets the maximum length of a component of a filename.
     */
    private int _nameMax;

    public int getNameMax() {
        return _nameMax;
    }

    public void setNameMax(int value) {
        _nameMax = value;
    }

    /**
     * Gets or sets a value indicating whether the server will reject any
     * request that includes a name longer than name_max with the error,
     * NFS3ERR_NAMETOOLONG.If FALSE, any length name over name_max bytes will be
     * silently truncated to name_max bytes.
     */
    private boolean _noTrunc;

    public boolean getNoTrunc() {
        return _noTrunc;
    }

    public void setNoTrunc(boolean value) {
        _noTrunc = value;
    }

    /**
     * Gets or sets a value indicating whether server will reject any request to
     * change either the owner or the group associated with a file if the caller
     * is not the privileged user. (Uid 0.)
     */
    private boolean _chownRestricted;

    public boolean getChownRestricted() {
        return _chownRestricted;
    }

    public void setChownRestricted(boolean value) {
        _chownRestricted = value;
    }

    /**
     * Gets or sets a value indicating whether the server file system does not
     * distinguish case when interpreting filenames.
     */
    private boolean _caseInsensitive;

    public boolean getCaseInsensitive() {
        return _caseInsensitive;
    }

    public void setCaseInsensitive(boolean value) {
        _caseInsensitive = value;
    }

    /**
     * Gets or sets a value indicating whether the server file system will
     * preserve the case of a name during a CREATE, MKDIR, MKNOD, SYMLINK,
     * RENAME, or LINK operation.
     */
    private boolean _casePreserving;

    public boolean getCasePreserving() {
        return _casePreserving;
    }

    public void setCasePreserving(boolean value) {
        _casePreserving = value;
    }

    public void write(XdrDataWriter writer) {
        writer.write(_status.getValue());
        _objectAttributes.write(writer);
        if (_status == Nfs3Status.Ok) {
            writer.write(_linkMax);
            writer.write(_nameMax);
            writer.write(_noTrunc);
            writer.write(_chownRestricted);
            writer.write(_caseInsensitive);
            writer.write(_casePreserving);
        }
    }

    public boolean equals(Object obj) {
        return equals(obj instanceof Nfs3PathConfResult ? (Nfs3PathConfResult) obj : (Nfs3PathConfResult) null);
    }

    public boolean equals(Nfs3PathConfResult other) {
        if (other == null) {
            return false;
        }

        return other._status == _status && other._linkMax == _linkMax && other._nameMax == _nameMax &&
               other._noTrunc == _noTrunc && other._chownRestricted == _chownRestricted &&
               other._caseInsensitive == _caseInsensitive && other._casePreserving == _casePreserving;
    }

    public int hashCode() {
        return dotnet4j.io.compat.Utilities.getCombinedHashCode(_status,
                                                                _linkMax,
                                                                _nameMax,
                                                                _noTrunc,
                                                                _chownRestricted,
                                                                _caseInsensitive,
                                                                _casePreserving);
    }
}
