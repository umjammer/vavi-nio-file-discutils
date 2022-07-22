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

package discUtils.core;

import java.util.EnumSet;

/**
 * Information about a file or directory common to most Unix systems.
 */
public final class UnixFileSystemInfo {

    /**
     * Gets or sets the device id of the referenced device (for character and
     * block devices).
     */
    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long value) {
        deviceId = value;
    }

    /**
     * Gets or sets the file's type.
     */
    private UnixFileType fileType = UnixFileType.None;

    public UnixFileType getFileType() {
        return fileType;
    }

    public void setFileType(UnixFileType value) {
        fileType = value;
    }

    /**
     * Gets or sets the group that owns this file or directory.
     */
    private int groupId;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int value) {
        groupId = value;
    }

    /**
     * Gets or sets the file's serial number (unique within file system).
     */
    private long inode;

    public long getInode() {
        return inode;
    }

    public void setInode(long value) {
        inode = value;
    }

    /**
     * Gets or sets the number of hard links to this file.
     */
    private int linkCount;

    public int getLinkCount() {
        return linkCount;
    }

    public void setLinkCount(int value) {
        linkCount = value;
    }

    /**
     * Gets or sets the file permissions (aka flags) for this file or directory.
     */
    private EnumSet<UnixFilePermissions> permissions;

    public EnumSet<UnixFilePermissions> getPermissions() {
        return permissions;
    }

    public void setPermissions(EnumSet<UnixFilePermissions> value) {
        permissions = value;
    }

    /**
     * Gets or sets the user that owns this file or directory.
     */
    private int userId;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int value) {
        userId = value;
    }
}
