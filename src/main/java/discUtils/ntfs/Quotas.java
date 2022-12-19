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

import java.io.PrintWriter;

import discUtils.streams.IByteArraySerializable;
import dotnet4j.security.principal.SecurityIdentifier;
import dotnet4j.security.principal.WellKnownSidType;
import dotnet4j.util.compat.Tuple;
import vavi.util.ByteUtil;
import vavi.util.win32.DateUtil;


public final class Quotas {

    private final IndexView<discUtils.ntfs.Quotas.OwnerKey, discUtils.ntfs.Quotas.OwnerRecord> ownerIndex;

    private final IndexView<discUtils.ntfs.Quotas.OwnerRecord, discUtils.ntfs.Quotas.QuotaRecord> quotaIndex;

    public Quotas(File file) {
        ownerIndex = new IndexView<>(discUtils.ntfs.Quotas.OwnerKey.class,
                                      discUtils.ntfs.Quotas.OwnerRecord.class,
                                      file.getIndex("$O"));
        quotaIndex = new IndexView<>(discUtils.ntfs.Quotas.OwnerRecord.class,
                                      discUtils.ntfs.Quotas.QuotaRecord.class,
                                      file.getIndex("$Q"));
    }

    public static Quotas initialize(File file) {
        Index ownerIndex = file.createIndex("$O", null, AttributeCollationRule.Sid);
        Index quotaIndox = file.createIndex("$Q", null, AttributeCollationRule.UnsignedLong);
        IndexView<discUtils.ntfs.Quotas.OwnerKey, discUtils.ntfs.Quotas.OwnerRecord> ownerIndexView = new IndexView<>(discUtils.ntfs.Quotas.OwnerKey.class,
                                                                                                                      discUtils.ntfs.Quotas.OwnerRecord.class,
                                                                                                                      ownerIndex);
        IndexView<discUtils.ntfs.Quotas.OwnerRecord, discUtils.ntfs.Quotas.QuotaRecord> quotaIndexView = new IndexView<>(discUtils.ntfs.Quotas.OwnerRecord.class,
                                                                                                                         discUtils.ntfs.Quotas.QuotaRecord.class,
                                                                                                                         quotaIndox);
        discUtils.ntfs.Quotas.OwnerKey adminSid = new discUtils.ntfs.Quotas.OwnerKey(new SecurityIdentifier(WellKnownSidType.BuiltinAdministratorsSid,
                                                                                                            null));
        discUtils.ntfs.Quotas.OwnerRecord adminOwnerId = new discUtils.ntfs.Quotas.OwnerRecord(256);
        ownerIndexView.put(adminSid, adminOwnerId);
        quotaIndexView.put(new discUtils.ntfs.Quotas.OwnerRecord(1), new discUtils.ntfs.Quotas.QuotaRecord(null));
        quotaIndexView.put(adminOwnerId, new discUtils.ntfs.Quotas.QuotaRecord(adminSid.sid));
        return new Quotas(file);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "QUOTAS");
        writer.println(indent + "  OWNER INDEX");
        for (Tuple<discUtils.ntfs.Quotas.OwnerKey, discUtils.ntfs.Quotas.OwnerRecord> entry : ownerIndex.getEntries()) {
            writer.println(indent + "    OWNER INDEX ENTRY");
            writer.println(indent + "            SID: " + entry.getKey().sid);
            writer.println(indent + "       Owner Id: " + entry.getValue().ownerId);
        }
        writer.println(indent + "  QUOTA INDEX");
        for (Tuple<discUtils.ntfs.Quotas.OwnerRecord, discUtils.ntfs.Quotas.QuotaRecord> entry : quotaIndex.getEntries()) {
            writer.println(indent + "    QUOTA INDEX ENTRY");
            writer.println(indent + "           Owner Id: " + entry.getKey().ownerId);
            writer.println(indent + "           User SID: " + entry.getValue().sid);
            writer.println(indent + "            Changed: " + entry.getValue().changeTime);
            writer.println(indent + "           Exceeded: " + entry.getValue().exceededTime);
            writer.println(indent + "         Bytes Used: " + entry.getValue().bytesUsed);
            writer.println(indent + "              flags: " + entry.getValue().flags);
            writer.println(indent + "         Hard Limit: " + entry.getValue().hardLimit);
            writer.println(indent + "      Warning Limit: " + entry.getValue().warningLimit);
            writer.println(indent + "            Version: " + entry.getValue().version);
        }
    }

    public final static class OwnerKey implements IByteArraySerializable {

        public SecurityIdentifier sid;

        public OwnerKey() {
        }

        public OwnerKey(SecurityIdentifier sid) {
            this.sid = sid;
        }

        public int size() {
            return sid.getBinaryLength();
        }

        public int readFrom(byte[] buffer, int offset) {
            sid = new SecurityIdentifier(buffer, offset);
            return sid.getBinaryLength();
        }

        public void writeTo(byte[] buffer, int offset) {
            sid.getBinaryForm(buffer, offset);
        }

        public String toString() {
            return String.format("[Sid:%s]", sid);
        }
    }

    public final static class OwnerRecord implements IByteArraySerializable {

        public int ownerId;

        public OwnerRecord() {
        }

        public OwnerRecord(int ownerId) {
            this.ownerId = ownerId;
        }

        public int size() {
            return 4;
        }

        public int readFrom(byte[] buffer, int offset) {
            ownerId = ByteUtil.readLeInt(buffer, offset);
            return 4;
        }

        public void writeTo(byte[] buffer, int offset) {
            ByteUtil.writeLeInt(ownerId, buffer, offset);
        }

        public String toString() {
            return "[OwnerId:" + ownerId + "]";
        }
    }

    public final static class QuotaRecord implements IByteArraySerializable {

        public long bytesUsed;

        public long changeTime;

        public long exceededTime;

        public int flags;

        public long hardLimit;

        public SecurityIdentifier sid;

        public int version;

        public long warningLimit;

        public QuotaRecord() {
        }

        public QuotaRecord(SecurityIdentifier sid) {
            version = 2;
            flags = 1;
            changeTime = System.currentTimeMillis();
            warningLimit = -1;
            hardLimit = -1;
            this.sid = sid;
        }

        public int size() {
            return 0x30 + (sid == null ? 0 : sid.getBinaryLength());
        }

        public int readFrom(byte[] buffer, int offset) {
            version = ByteUtil.readLeInt(buffer, offset);
            flags = ByteUtil.readLeInt(buffer, offset + 0x04);
            bytesUsed = ByteUtil.readLeLong(buffer, offset + 0x08);
            changeTime = DateUtil.fromFileTime(ByteUtil.readLeLong(buffer, offset + 0x10));
            warningLimit = ByteUtil.readLeLong(buffer, offset + 0x18);
            hardLimit = ByteUtil.readLeLong(buffer, offset + 0x20);
            exceededTime = ByteUtil.readLeLong(buffer, offset + 0x28);
            if (buffer.length - offset > 0x30) {
                sid = new SecurityIdentifier(buffer, offset + 0x30);
                return 0x30 + sid.getBinaryLength();
            }

            return 0x30;
        }

        public void writeTo(byte[] buffer, int offset) {
            ByteUtil.writeLeInt(version, buffer, offset);
            ByteUtil.writeLeInt(flags, buffer, offset + 0x04);
            ByteUtil.writeLeLong(bytesUsed, buffer, offset + 0x08);
            ByteUtil.writeLeLong(DateUtil.toFileTime(changeTime), buffer, offset + 0x10);
            ByteUtil.writeLeLong(warningLimit, buffer, offset + 0x18);
            ByteUtil.writeLeLong(hardLimit, buffer, offset + 0x20);
            ByteUtil.writeLeLong(exceededTime, buffer, offset + 0x28);
            if (sid != null) {
                sid.getBinaryForm(buffer, offset + 0x30);
            }
        }

        public String toString() {
            return "[V:" + version + ",F:" + flags + ",BU:" + bytesUsed + ",CT:" + changeTime + ",WL:" + warningLimit + ",HL:" +
                    hardLimit + ",ET:" + exceededTime + ",SID:" + sid + "]";
        }
    }
}
