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

import vavi.util.win32.DateUtil;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import dotnet4j.Tuple;
import dotnet4j.security.principal.SecurityIdentifier;
import dotnet4j.security.principal.WellKnownSidType;


public final class Quotas {
    private final IndexView<discUtils.ntfs.Quotas.OwnerKey, discUtils.ntfs.Quotas.OwnerRecord> _ownerIndex;

    private final IndexView<discUtils.ntfs.Quotas.OwnerRecord, discUtils.ntfs.Quotas.QuotaRecord> _quotaIndex;

    public Quotas(File file) {
        _ownerIndex = new IndexView<>(discUtils.ntfs.Quotas.OwnerKey.class,
                                      discUtils.ntfs.Quotas.OwnerRecord.class,
                                      file.getIndex("$O"));
        _quotaIndex = new IndexView<>(discUtils.ntfs.Quotas.OwnerRecord.class,
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
        quotaIndexView.put(adminOwnerId, new discUtils.ntfs.Quotas.QuotaRecord(adminSid._sid));
        return new Quotas(file);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "QUOTAS");
        writer.println(indent + "  OWNER INDEX");
        for (Tuple<discUtils.ntfs.Quotas.OwnerKey, discUtils.ntfs.Quotas.OwnerRecord> entry : _ownerIndex.getEntries()) {
            writer.println(indent + "    OWNER INDEX ENTRY");
            writer.println(indent + "            SID: " + entry.getKey()._sid);
            writer.println(indent + "       Owner Id: " + entry.getValue()._ownerId);
        }
        writer.println(indent + "  QUOTA INDEX");
        for (Tuple<discUtils.ntfs.Quotas.OwnerRecord, discUtils.ntfs.Quotas.QuotaRecord> entry : _quotaIndex.getEntries()) {
            writer.println(indent + "    QUOTA INDEX ENTRY");
            writer.println(indent + "           Owner Id: " + entry.getKey()._ownerId);
            writer.println(indent + "           User SID: " + entry.getValue()._sid);
            writer.println(indent + "            Changed: " + entry.getValue()._changeTime);
            writer.println(indent + "           Exceeded: " + entry.getValue()._exceededTime);
            writer.println(indent + "         Bytes Used: " + entry.getValue()._bytesUsed);
            writer.println(indent + "              Flags: " + entry.getValue()._flags);
            writer.println(indent + "         Hard Limit: " + entry.getValue()._hardLimit);
            writer.println(indent + "      Warning Limit: " + entry.getValue()._warningLimit);
            writer.println(indent + "            Version: " + entry.getValue()._version);
        }
    }

    public final static class OwnerKey implements IByteArraySerializable {
        public SecurityIdentifier _sid;

        public OwnerKey() {
        }

        public OwnerKey(SecurityIdentifier sid) {
            _sid = sid;
        }

        public int size() {
            return _sid.getBinaryLength();
        }

        public int readFrom(byte[] buffer, int offset) {
            _sid = new SecurityIdentifier(buffer, offset);
            return _sid.getBinaryLength();
        }

        public void writeTo(byte[] buffer, int offset) {
            _sid.getBinaryForm(buffer, offset);
        }

        public String toString() {
            return String.format("[Sid:%s]", _sid);
        }
    }

    public final static class OwnerRecord implements IByteArraySerializable {
        public int _ownerId;

        public OwnerRecord() {
        }

        public OwnerRecord(int ownerId) {
            _ownerId = ownerId;
        }

        public int size() {
            return 4;
        }

        public int readFrom(byte[] buffer, int offset) {
            _ownerId = EndianUtilities.toInt32LittleEndian(buffer, offset);
            return 4;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(_ownerId, buffer, offset);
        }

        public String toString() {
            return "[OwnerId:" + _ownerId + "]";
        }
    }

    public final static class QuotaRecord implements IByteArraySerializable {
        public long _bytesUsed;

        public long _changeTime;

        public long _exceededTime;

        public int _flags;

        public long _hardLimit;

        public SecurityIdentifier _sid;

        public int _version;

        public long _warningLimit;

        public QuotaRecord() {
        }

        public QuotaRecord(SecurityIdentifier sid) {
            _version = 2;
            _flags = 1;
            _changeTime = System.currentTimeMillis();
            _warningLimit = -1;
            _hardLimit = -1;
            _sid = sid;
        }

        public int size() {
            return 0x30 + (_sid == null ? 0 : _sid.getBinaryLength());
        }

        public int readFrom(byte[] buffer, int offset) {
            _version = EndianUtilities.toInt32LittleEndian(buffer, offset);
            _flags = EndianUtilities.toInt32LittleEndian(buffer, offset + 0x04);
            _bytesUsed = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x08);
            _changeTime = DateUtil.fromFileTime(EndianUtilities.toInt64LittleEndian(buffer, offset + 0x10));
            _warningLimit = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x18);
            _hardLimit = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x20);
            _exceededTime = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x28);
            if (buffer.length - offset > 0x30) {
                _sid = new SecurityIdentifier(buffer, offset + 0x30);
                return 0x30 + _sid.getBinaryLength();
            }

            return 0x30;
        }

        public void writeTo(byte[] buffer, int offset) {
            EndianUtilities.writeBytesLittleEndian(_version, buffer, offset);
            EndianUtilities.writeBytesLittleEndian(_flags, buffer, offset + 0x04);
            EndianUtilities.writeBytesLittleEndian(_bytesUsed, buffer, offset + 0x08);
            EndianUtilities
                    .writeBytesLittleEndian(DateUtil.toFileTime(_changeTime), buffer, offset + 0x10);
            EndianUtilities.writeBytesLittleEndian(_warningLimit, buffer, offset + 0x18);
            EndianUtilities.writeBytesLittleEndian(_hardLimit, buffer, offset + 0x20);
            EndianUtilities.writeBytesLittleEndian(_exceededTime, buffer, offset + 0x28);
            if (_sid != null) {
                _sid.getBinaryForm(buffer, offset + 0x30);
            }
        }

        public String toString() {
            return "[V:" + _version + ",F:" + _flags + ",BU:" + _bytesUsed + ",CT:" + _changeTime + ",WL:" + _warningLimit + ",HL:" +
                   _hardLimit + ",ET:" + _exceededTime + ",SID:" + _sid + "]";
        }
    }
}
