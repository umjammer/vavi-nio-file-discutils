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

import java.io.PrintWriter;
import java.time.Instant;

import vavi.util.win32.DateUtil;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import dotnet4j.Tuple;
import dotnet4j.security.principal.SecurityIdentifier;
import dotnet4j.security.principal.WellKnownSidType;


public final class Quotas {
    private final IndexView<DiscUtils.Ntfs.Quotas.OwnerKey, DiscUtils.Ntfs.Quotas.OwnerRecord> _ownerIndex;

    private final IndexView<DiscUtils.Ntfs.Quotas.OwnerRecord, DiscUtils.Ntfs.Quotas.QuotaRecord> _quotaIndex;

    public Quotas(File file) {
        _ownerIndex = new IndexView<>(DiscUtils.Ntfs.Quotas.OwnerKey.class,
                                      DiscUtils.Ntfs.Quotas.OwnerRecord.class,
                                      file.getIndex("$O"));
        _quotaIndex = new IndexView<>(DiscUtils.Ntfs.Quotas.OwnerRecord.class,
                                      DiscUtils.Ntfs.Quotas.QuotaRecord.class,
                                      file.getIndex("$Q"));
    }

    public static Quotas initialize(File file) {
        Index ownerIndex = file.createIndex("$O", null, AttributeCollationRule.Sid);
        Index quotaIndox = file.createIndex("$Q", null, AttributeCollationRule.UnsignedLong);
        IndexView<DiscUtils.Ntfs.Quotas.OwnerKey, DiscUtils.Ntfs.Quotas.OwnerRecord> ownerIndexView = new IndexView<>(DiscUtils.Ntfs.Quotas.OwnerKey.class,
                                                                                                                      DiscUtils.Ntfs.Quotas.OwnerRecord.class,
                                                                                                                      ownerIndex);
        IndexView<DiscUtils.Ntfs.Quotas.OwnerRecord, DiscUtils.Ntfs.Quotas.QuotaRecord> quotaIndexView = new IndexView<>(DiscUtils.Ntfs.Quotas.OwnerRecord.class,
                                                                                                                         DiscUtils.Ntfs.Quotas.QuotaRecord.class,
                                                                                                                         quotaIndox);
        DiscUtils.Ntfs.Quotas.OwnerKey adminSid = new DiscUtils.Ntfs.Quotas.OwnerKey(new SecurityIdentifier(WellKnownSidType.BuiltinAdministratorsSid,
                                                                                                            null));
        DiscUtils.Ntfs.Quotas.OwnerRecord adminOwnerId = new DiscUtils.Ntfs.Quotas.OwnerRecord(256);
        ownerIndexView.set___idx(adminSid, adminOwnerId);
        quotaIndexView.set___idx(new DiscUtils.Ntfs.Quotas.OwnerRecord(1), new DiscUtils.Ntfs.Quotas.QuotaRecord(null));
        quotaIndexView.set___idx(adminOwnerId, new DiscUtils.Ntfs.Quotas.QuotaRecord(adminSid.Sid));
        return new Quotas(file);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "QUOTAS");
        writer.println(indent + "  OWNER INDEX");
        for (Tuple<DiscUtils.Ntfs.Quotas.OwnerKey, DiscUtils.Ntfs.Quotas.OwnerRecord> entry : _ownerIndex.getEntries()) {
            writer.println(indent + "    OWNER INDEX ENTRY");
            writer.println(indent + "            SID: " + entry.getKey().Sid);
            writer.println(indent + "       Owner Id: " + entry.getValue().OwnerId);
        }
        writer.println(indent + "  QUOTA INDEX");
        for (Tuple<DiscUtils.Ntfs.Quotas.OwnerRecord, DiscUtils.Ntfs.Quotas.QuotaRecord> entry : _quotaIndex.getEntries()) {
            writer.println(indent + "    QUOTA INDEX ENTRY");
            writer.println(indent + "           Owner Id: " + entry.getKey().OwnerId);
            writer.println(indent + "           User SID: " + entry.getValue().Sid);
            writer.println(indent + "            Changed: " + entry.getValue().ChangeTime);
            writer.println(indent + "           Exceeded: " + entry.getValue().ExceededTime);
            writer.println(indent + "         Bytes Used: " + entry.getValue().BytesUsed);
            writer.println(indent + "              Flags: " + entry.getValue().Flags);
            writer.println(indent + "         Hard Limit: " + entry.getValue().HardLimit);
            writer.println(indent + "      Warning Limit: " + entry.getValue().WarningLimit);
            writer.println(indent + "            Version: " + entry.getValue().Version);
        }
    }

    public final static class OwnerKey implements IByteArraySerializable {
        public SecurityIdentifier Sid;

        public OwnerKey() {
        }

        public OwnerKey(SecurityIdentifier sid) {
            Sid = sid;
        }

        public int size() {
            return Sid.getBinaryLength();
        }

        public int readFrom(byte[] buffer, int offset) {
            Sid = new SecurityIdentifier(buffer, offset);
            return Sid.getBinaryLength();
        }

        public void writeTo(byte[] buffer, int offset) {
            Sid.getBinaryForm(buffer, offset);
        }

        public String toString() {
            return String.format("[Sid:%s]", Sid);
        }
    }

    public final static class OwnerRecord implements IByteArraySerializable {
        public int OwnerId;

        public OwnerRecord() {
        }

        public OwnerRecord(int ownerId) {
            OwnerId = ownerId;
        }

        public int size() {
            return 4;
        }

        public int readFrom(byte[] buffer, int offset) {
            OwnerId = EndianUtilities.toInt32LittleEndian(buffer, offset);
            return 4;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(OwnerId, buffer, offset);
        }

        public String toString() {
            return "[OwnerId:" + OwnerId + "]";
        }
    }

    public final static class QuotaRecord implements IByteArraySerializable {
        public long BytesUsed;

        public long ChangeTime;

        public long ExceededTime;

        public int Flags;

        public long HardLimit;

        public SecurityIdentifier Sid;

        public int Version;

        public long WarningLimit;

        public QuotaRecord() {
        }

        public QuotaRecord(SecurityIdentifier sid) {
            Version = 2;
            Flags = 1;
            ChangeTime = System.currentTimeMillis();
            WarningLimit = -1;
            HardLimit = -1;
            Sid = sid;
        }

        public int size() {
            return 0x30 + (Sid == null ? 0 : Sid.getBinaryLength());
        }

        public int readFrom(byte[] buffer, int offset) {
            Version = EndianUtilities.toInt32LittleEndian(buffer, offset);
            Flags = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x04);
            BytesUsed = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x08);
            ChangeTime = DateUtil.filetimeToLong(EndianUtilities.toInt64LittleEndian(buffer, offset + 0x10));
            WarningLimit = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x18);
            HardLimit = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x20);
            ExceededTime = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x28);
            if (buffer.length - offset > 0x30) {
                Sid = new SecurityIdentifier(buffer, offset + 0x30);
                return 0x30 + Sid.getBinaryLength();
            }

            return 0x30;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(Version, buffer, offset);
            EndianUtilities.writeBytesLittleEndian(Flags, buffer, offset + 0x04);
            EndianUtilities.writeBytesLittleEndian(BytesUsed, buffer, offset + 0x08);
            EndianUtilities
                    .writeBytesLittleEndian(DateUtil.toFileTime(Instant.ofEpochMilli(ChangeTime)), buffer, offset + 0x10);
            EndianUtilities.writeBytesLittleEndian(WarningLimit, buffer, offset + 0x18);
            EndianUtilities.writeBytesLittleEndian(HardLimit, buffer, offset + 0x20);
            EndianUtilities.writeBytesLittleEndian(ExceededTime, buffer, offset + 0x28);
            if (Sid != null) {
                Sid.getBinaryForm(buffer, offset + 0x30);
            }

        }

        public String toString() {
            return "[V:" + Version + ",F:" + Flags + ",BU:" + BytesUsed + ",CT:" + ChangeTime + ",WL:" + WarningLimit + ",HL:" +
                   HardLimit + ",ET:" + ExceededTime + ",SID:" + Sid + "]";
        }
    }
}
