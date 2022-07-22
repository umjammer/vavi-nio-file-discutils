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

package discUtils.squashFs;

import java.util.EnumSet;

import discUtils.core.UnixFilePermissions;


public abstract class BuilderNode {
    protected boolean written;

    public BuilderNode() {
        setModificationTime(System.currentTimeMillis());
    }

    private int groupId;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int value) {
        groupId = value;
    }

    public abstract Inode getInode();

    private int inodeNumber;

    public int getInodeNumber() {
        return inodeNumber;
    }

    public void setInodeNumber(int value) {
        inodeNumber = value;
    }

    private MetadataRef inodeRef;

    public MetadataRef getInodeRef() {
        return inodeRef;
    }

    public void setInodeRef(MetadataRef value) {
        inodeRef = value;
    }

    private EnumSet<UnixFilePermissions> mode = EnumSet.noneOf(UnixFilePermissions.class);

    public EnumSet<UnixFilePermissions> getMode() {
        return mode;
    }

    public void setMode(EnumSet<UnixFilePermissions> value) {
        mode = value;
    }

    private long modificationTime;

    public long getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(long value) {
        modificationTime = value;
    }

    private int numLinks;

    public int getNumLinks() {
        return numLinks;
    }

    public void setNumLinks(int value) {
        numLinks = value;
    }

    private int userId;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int value) {
        userId = value;
    }

    public void reset() {
        written = false;
    }

    public abstract void write(BuilderContext context);

    protected void fillCommonInodeData(BuilderContext context) {
        getInode().mode = (short) UnixFilePermissions.valueOf(getMode());
        getInode().uidKey = context.getAllocateId().invoke(getUserId());
        getInode().gidKey = context.getAllocateId().invoke(getGroupId());
        getInode().modificationTime = getModificationTime();
        setInodeNumber(context.getAllocateInode().invoke());
        getInode().inodeNumber = getInodeNumber();
        getInode().numLinks = getNumLinks();
    }
}
