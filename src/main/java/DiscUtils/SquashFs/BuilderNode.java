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

package DiscUtils.SquashFs;

import java.util.EnumSet;

import DiscUtils.Core.UnixFilePermissions;


public abstract class BuilderNode {
    protected boolean _written;

    public BuilderNode() {
        setModificationTime(System.currentTimeMillis());
    }

    private int __GroupId;

    public int getGroupId() {
        return __GroupId;
    }

    public void setGroupId(int value) {
        __GroupId = value;
    }

    public abstract Inode getInode();

    private int __InodeNumber;

    public int getInodeNumber() {
        return __InodeNumber;
    }

    public void setInodeNumber(int value) {
        __InodeNumber = value;
    }

    private MetadataRef __InodeRef;

    public MetadataRef getInodeRef() {
        return __InodeRef;
    }

    public void setInodeRef(MetadataRef value) {
        __InodeRef = value;
    }

    private EnumSet<UnixFilePermissions> __Mode = EnumSet.noneOf(UnixFilePermissions.class);

    public EnumSet<UnixFilePermissions> getMode() {
        return __Mode;
    }

    public void setMode(EnumSet<UnixFilePermissions> value) {
        __Mode = value;
    }

    private long __ModificationTime;

    public long getModificationTime() {
        return __ModificationTime;
    }

    public void setModificationTime(long value) {
        __ModificationTime = value;
    }

    private int __NumLinks;

    public int getNumLinks() {
        return __NumLinks;
    }

    public void setNumLinks(int value) {
        __NumLinks = value;
    }

    private int __UserId;

    public int getUserId() {
        return __UserId;
    }

    public void setUserId(int value) {
        __UserId = value;
    }

    public void reset() {
        _written = false;
    }

    public abstract void write(BuilderContext context);

    protected void fillCommonInodeData(BuilderContext context) {
        getInode().Mode = (short) UnixFilePermissions.valueOf(getMode());
        getInode().UidKey = context.getAllocateId().invoke(getUserId());
        getInode().GidKey = context.getAllocateId().invoke(getGroupId());
        getInode().ModificationTime = getModificationTime();
        setInodeNumber(context.getAllocateInode().invoke());
        getInode().InodeNumber = getInodeNumber();
        getInode().NumLinks = getNumLinks();
    }
}
