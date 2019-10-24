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

package DiscUtils.Iscsi;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import DiscUtils.Core.CoreCompat.ReflectionHelper;
import dotnet4j.io.FileAccess;


/**
 * Represents a connection to a particular Target.
 */
public final class Session implements Closeable {
    private static AtomicInteger _nextInitiatorSessionId = new AtomicInteger(new SecureRandom().nextInt());

    private final List<TargetAddress> _addresses;

    /**
     * The set of all 'parameters' we've negotiated.
     */
    private final Map<String, String> _negotiatedParameters;

    private short _nextConnectionId;

    public Session(SessionType type, String targetName, List<TargetAddress> addresses) {
        this(type, targetName, null, null, addresses);
    }

    public Session(SessionType type, String targetName, String userName, String password, List<TargetAddress> addresses) {
        __InitiatorSessionId = _nextInitiatorSessionId.incrementAndGet();
        _addresses = addresses;
        setSessionType(type);
        setTargetName(targetName);
        setCommandSequenceNumber(1);
        setCurrentTaskTag(1);
        // Default negotiated values...
        setMaxConnections(1);
        setInitialR2T(true);
        setImmediateData(true);
        setMaxBurstLength(262144);
        setFirstBurstLength(65536);
        setDefaultTime2Wait(0);
        setDefaultTime2Retain(60);
        setMaxOutstandingR2T(1);
        setDataPDUInOrder(true);
        setDataSequenceInOrder(true);
        _negotiatedParameters = new HashMap<>();
        if (userName == null || userName.isEmpty()) {
            setActiveConnection(new Connection(this,
                                               _addresses.get(0),
                                               new Authenticator[] {
                                                   new NullAuthenticator()
                                               }));
        } else {
            setActiveConnection(new Connection(this,
                                               _addresses.get(0),
                                               new Authenticator[] {
                                                   new NullAuthenticator(), new ChapAuthenticator(userName, password)
                                               }));
        }
    }

    private Connection __ActiveConnection;

    public Connection getActiveConnection() {
        return __ActiveConnection;
    }

    public void setActiveConnection(Connection value) {
        __ActiveConnection = value;
    }

    private int __CommandSequenceNumber;

    public int getCommandSequenceNumber() {
        return __CommandSequenceNumber;
    }

    public void setCommandSequenceNumber(int value) {
        __CommandSequenceNumber = value;
    }

    private int __CurrentTaskTag;

    public int getCurrentTaskTag() {
        return __CurrentTaskTag;
    }

    public void setCurrentTaskTag(int value) {
        __CurrentTaskTag = value;
    }

    private int __InitiatorSessionId;

    public int getInitiatorSessionId() {
        return __InitiatorSessionId;
    }

    private short __TargetSessionId;

    public short getTargetSessionId() {
        return __TargetSessionId;
    }

    public void setTargetSessionId(short value) {
        __TargetSessionId = value;
    }

    /**
     * Disposes of this instance, closing the session with the Target.
     */
    public void close() throws IOException {
        if (getActiveConnection() != null) {
            getActiveConnection().close();
        }

        setActiveConnection(null);
    }

    /**
     * Enumerates all of the Targets.
     *
     * @return The list of Targets.In practice, for an established session, this
     *         just returns details of
     *         the connected Target.
     */
    public TargetInfo[] enumerateTargets() {
        return getActiveConnection().EnumerateTargets();
    }

    /**
     * Gets information about the LUNs available from the Target.
     *
     * @return The LUNs available.
     */
    public LunInfo[] getLuns() {
        ScsiReportLunsCommand cmd = new ScsiReportLunsCommand(ScsiReportLunsCommand.InitialResponseSize);
        ScsiReportLunsResponse resp = send(ScsiReportLunsResponse.class, cmd, null, 0, 0, ScsiReportLunsCommand.InitialResponseSize);
        if (resp.getTruncated()) {
            cmd = new ScsiReportLunsCommand(resp.getNeededDataLength());
            resp = send(ScsiReportLunsResponse.class, cmd, null, 0, 0, resp.getNeededDataLength());
        }

        if (resp.getTruncated()) {
            throw new IllegalArgumentException("Truncated response");
        }

        LunInfo[] result = new LunInfo[resp.getLuns().size()];
        for (int i = 0; i < resp.getLuns().size(); ++i) {
            result[i] = getInfo(resp.getLuns().get(i));
        }
        return result;
    }

    /**
     * Gets all the block-device LUNs available from the Target.
     *
     * @return The block-device LUNs.
     */
    public List<Long> getBlockDeviceLuns() {
        List<Long> luns = new ArrayList<>();
        for (LunInfo info : getLuns()) {
            if (info.getDeviceType() == LunClass.BlockStorage) {
                luns.add(info.getLun());
            }
        }
        return luns;
    }

    /**
     * Gets information about a particular LUN.
     *
     * @param lun The LUN to query.
     * @return Information about the LUN.
     */
    public LunInfo getInfo(long lun) {
        ScsiInquiryCommand cmd = new ScsiInquiryCommand(lun, ScsiInquiryCommand.InitialResponseDataLength);
        ScsiInquiryStandardResponse resp = send(ScsiInquiryStandardResponse.class, cmd, null, 0, 0, ScsiInquiryCommand.InitialResponseDataLength);
        TargetInfo targetInfo = new TargetInfo(getTargetName(), _addresses);
        return new LunInfo(targetInfo,
                           lun,
                           resp.getDeviceType(),
                           resp.getRemovable(),
                           resp.getVendorId(),
                           resp.getProductId(),
                           resp.getProductRevision());
    }

    /**
     * Gets the capacity of a particular LUN.
     *
     * @param lun The LUN to query.
     * @return The LUN's capacity.
     */
    public LunCapacity getCapacity(long lun) {
        ScsiReadCapacityCommand cmd = new ScsiReadCapacityCommand(lun);
        ScsiReadCapacityResponse resp = send(ScsiReadCapacityResponse.class, cmd, null, 0, 0, ScsiReadCapacityCommand.ResponseDataLength);
        if (resp.getTruncated()) {
            throw new IllegalArgumentException("Truncated response");
        }

        return new LunCapacity(resp.getNumLogicalBlocks(), resp.getLogicalBlockSize());
    }

    /**
     * Provides read-write access to a LUN as a VirtualDisk.
     *
     * @param lun The LUN to access.
     * @return The new VirtualDisk instance.
     */
    public Disk openDisk(long lun) {
        return openDisk(lun, FileAccess.ReadWrite);
    }

    /**
     * Provides access to a LUN as a VirtualDisk.
     *
     * @param lun The LUN to access.
     * @param access The type of access desired.
     * @return The new VirtualDisk instance.
     */
    public Disk openDisk(long lun, FileAccess access) {
        return new Disk(this, lun, access);
    }

    /**
     * Reads some data from a LUN.
     *
     * @param lun The LUN to read from.
     * @param startBlock The first block to read.
     * @param blockCount The number of blocks to read.
     * @param buffer The buffer to fill.
     * @param offset The offset of the first byte to fill.
     * @return The number of bytes read.
     */
    public int read(long lun, long startBlock, short blockCount, byte[] buffer, int offset) {
        ScsiReadCommand cmd = new ScsiReadCommand(lun, (int) startBlock, blockCount);
        return send(cmd, null, 0, 0, buffer, offset, buffer.length - offset);
    }

    /**
     * Writes some data to a LUN.
     *
     * @param lun The LUN to write to.
     * @param startBlock The first block to write.
     * @param blockCount The number of blocks to write.
     * @param blockSize The size of each block (must match the actual LUN
     *            geometry).
     * @param buffer The data to write.
     * @param offset The offset of the first byte to write in buffer.
     */
    public void write(long lun, long startBlock, short blockCount, int blockSize, byte[] buffer, int offset) {
        ScsiWriteCommand cmd = new ScsiWriteCommand(lun, (int) startBlock, blockCount);
        send(cmd, buffer, offset, blockCount * blockSize, null, 0, 0);
    }

    /**
     * Performs a raw SCSI command.
     *
     * @param lun The target LUN for the command.
     * @param command The command (a SCSI Command Descriptor Block, aka CDB).
     * @param outBuffer Buffer of data to send with the command (or
     *            {@code null}
     *            ).
     * @param outBufferOffset Offset of first byte of data to send with the
     *            command.
     * @param outBufferLength Amount of data to send with the command.
     * @param inBuffer Buffer to receive data from the command (or
     *            {@code null}
     *            ).
     * @param inBufferOffset Offset of the first byte position to fill with
     *            received data.
     * @param inBufferLength The expected amount of data to receive.
     * @return The number of bytes of data received.This method permits the
     *         caller to send raw SCSI commands to a LUN.The command .
     */
    public int rawCommand(long lun,
                          byte[] command,
                          byte[] outBuffer,
                          int outBufferOffset,
                          int outBufferLength,
                          byte[] inBuffer,
                          int inBufferOffset,
                          int inBufferLength) {
        if (outBuffer == null && outBufferLength != 0) {
            throw new IllegalArgumentException("outBufferLength must be 0 if outBuffer null");
        }

        if (inBuffer == null && inBufferLength != 0) {
            throw new IllegalArgumentException("inBufferLength must be 0 if inBuffer null");
        }

        ScsiRawCommand cmd = new ScsiRawCommand(lun, command, 0, command.length);
        return send(cmd, outBuffer, outBufferOffset, outBufferLength, inBuffer, inBufferOffset, inBufferLength);
    }

    public int nextCommandSequenceNumber() {
        setCommandSequenceNumber(getCommandSequenceNumber() + 1);
        return getCommandSequenceNumber();
    }

    public int nextTaskTag() {
        setCurrentTaskTag(getCurrentTaskTag() + 1);
        return getCurrentTaskTag();
    }

    public short nextConnectionId() {
        return ++_nextConnectionId;
    }

    public void getParametersToNegotiate(TextBuffer parameters, KeyUsagePhase phase) {
        try {
            for (Field propInfo : getClass().getDeclaredFields()) {
                ProtocolKeyAttribute attr = ReflectionHelper.getCustomAttribute(propInfo, ProtocolKeyAttribute.class);
                if (attr != null) {
                    Object value = propInfo.get(this);
                    if (ProtocolKeyAttribute.Util.shouldTransmit(attr, value, propInfo.getType(), phase, getSessionType() == SessionType.Discovery)) {
                        parameters.add(attr.name(), ProtocolKeyAttribute.Util.getValueAsString(value, propInfo.getType()));
                        _negotiatedParameters.put(attr.name(), "");
                    }
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public void consumeParameters(TextBuffer inParameters, TextBuffer outParameters) {
        try {
            for (Field propInfo : getClass().getDeclaredFields()) {
                ProtocolKeyAttribute attr = ReflectionHelper.getCustomAttribute(propInfo, ProtocolKeyAttribute.class);
                if (attr != null) {
                    if (inParameters.get___idx(attr.name()) != null) {
                        Object value = ProtocolKeyAttribute.Util.getValueAsObject(inParameters.get___idx(attr.name()),
                                                                             propInfo.getType());
                        propInfo.set(this, value);
                        inParameters.remove(attr.name());

                        if (attr.type() == KeyType.Negotiated && !_negotiatedParameters.containsKey(attr.name())) {
                            value = propInfo.get(this);
                            outParameters.add(attr.name(), ProtocolKeyAttribute.Util.getValueAsString(value, propInfo.getType()));
                            _negotiatedParameters.put(attr.name(), "");
                        }
                    }
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Gets the name of the iSCSI target this session is connected to.
     */
    @ProtocolKeyAttribute(name = "TargetName",
                          phase = KeyUsagePhase.SecurityNegotiation,
                          sender = KeySender.Initiator,
                          type = KeyType.Declarative,
                          usedForDiscovery = true)
    private String __TargetName;

    public String getTargetName() {
        return __TargetName;
    }

    public void setTargetName(String value) {
        __TargetName = value;
    }

    /**
     * Gets the name of the iSCSI initiator seen by the target for this session.
     */
    @ProtocolKeyAttribute(name = "InitiatorName",
                          phase = KeyUsagePhase.SecurityNegotiation,
                          sender = KeySender.Initiator,
                          type = KeyType.Declarative,
                          usedForDiscovery = true)
    public String getInitiatorName() {
        return "iqn.2008-2010-04.discutils.codeplex.com";
    }

    /**
     * Gets the friendly name of the iSCSI target this session is connected to.
     */
    @ProtocolKeyAttribute(name = "TargetAlias",
                          defaultValue = "",
                          phase = KeyUsagePhase.All,
                          sender = KeySender.Target,
                          type = KeyType.Declarative)
    private String __TargetAlias;

    public String getTargetAlias() {
        return __TargetAlias;
    }

    public void setTargetAlias(String value) {
        __TargetAlias = value;
    }

    @ProtocolKeyAttribute(name = "SessionType",
                          phase = KeyUsagePhase.SecurityNegotiation,
                          sender = KeySender.Initiator,
                          type = KeyType.Declarative,
                          usedForDiscovery = true)
    private SessionType __SessionType = SessionType.Discovery;

    public SessionType getSessionType() {
        return __SessionType;
    }

    public void setSessionType(SessionType value) {
        __SessionType = value;
    }

    @ProtocolKeyAttribute(name = "MaxConnections",
                          defaultValue = "1",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int __MaxConnections;

    public int getMaxConnections() {
        return __MaxConnections;
    }

    public void setMaxConnections(int value) {
        __MaxConnections = value;
    }

    @ProtocolKeyAttribute(name = "InitiatorAlias",
                          defaultValue = "",
                          phase = KeyUsagePhase.All,
                          sender = KeySender.Initiator,
                          type = KeyType.Declarative)
    private String __InitiatorAlias;

    public String getInitiatorAlias() {
        return __InitiatorAlias;
    }

    public void setInitiatorAlias(String value) {
        __InitiatorAlias = value;
    }

    @ProtocolKeyAttribute(name = "TargetPortalGroupTag",
                          phase = KeyUsagePhase.SecurityNegotiation,
                          sender = KeySender.Target,
                          type = KeyType.Declarative)
    private int __TargetPortalGroupTag;

    public int getTargetPortalGroupTag() {
        return __TargetPortalGroupTag;
    }

    public void setTargetPortalGroupTag(int value) {
        __TargetPortalGroupTag = value;
    }

    @ProtocolKeyAttribute(name = "InitialR2T",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private boolean __InitialR2T;

    public boolean getInitialR2T() {
        return __InitialR2T;
    }

    public void setInitialR2T(boolean value) {
        __InitialR2T = value;
    }

    @ProtocolKeyAttribute(name = "ImmediateData",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private boolean __ImmediateData;

    public boolean getImmediateData() {
        return __ImmediateData;
    }

    public void setImmediateData(boolean value) {
        __ImmediateData = value;
    }

    @ProtocolKeyAttribute(name = "MaxBurstLength",
                          defaultValue = "262144",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int __MaxBurstLength;

    public int getMaxBurstLength() {
        return __MaxBurstLength;
    }

    public void setMaxBurstLength(int value) {
        __MaxBurstLength = value;
    }

    @ProtocolKeyAttribute(name = "FirstBurstLength",
                          defaultValue = "65536",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int __FirstBurstLength;

    public int getFirstBurstLength() {
        return __FirstBurstLength;
    }

    public void setFirstBurstLength(int value) {
        __FirstBurstLength = value;
    }

    @ProtocolKeyAttribute(name = "DefaultTime2Wait",
                          defaultValue = "2",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int __DefaultTime2Wait;

    public int getDefaultTime2Wait() {
        return __DefaultTime2Wait;
    }

    public void setDefaultTime2Wait(int value) {
        __DefaultTime2Wait = value;
    }

    @ProtocolKeyAttribute(name = "DefaultTime2Retain",
                          defaultValue = "20",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int __DefaultTime2Retain;

    public int getDefaultTime2Retain() {
        return __DefaultTime2Retain;
    }

    public void setDefaultTime2Retain(int value) {
        __DefaultTime2Retain = value;
    }

    @ProtocolKeyAttribute(name = "MaxOutstandingR2T",
                          defaultValue = "1",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int __MaxOutstandingR2T;

    public int getMaxOutstandingR2T() {
        return __MaxOutstandingR2T;
    }

    public void setMaxOutstandingR2T(int value) {
        __MaxOutstandingR2T = value;
    }

    @ProtocolKeyAttribute(name = "DataPDUInOrder",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private boolean __DataPDUInOrder;

    public boolean getDataPDUInOrder() {
        return __DataPDUInOrder;
    }

    public void setDataPDUInOrder(boolean value) {
        __DataPDUInOrder = value;
    }

    @ProtocolKeyAttribute(name = "DataSequenceInOrder",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private boolean __DataSequenceInOrder;

    public boolean getDataSequenceInOrder() {
        return __DataSequenceInOrder;
    }

    public void setDataSequenceInOrder(boolean value) {
        __DataSequenceInOrder = value;
    }

    @ProtocolKeyAttribute(name = "ErrorRecoveryLevel",
                          defaultValue = "0",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int __ErrorRecoveryLevel;

    public int getErrorRecoveryLevel() {
        return __ErrorRecoveryLevel;
    }

    public void setErrorRecoveryLevel(int value) {
        __ErrorRecoveryLevel = value;
    }

    /**
     * Sends an SCSI command (aka task) to a LUN via the connected target.
     *
     * @param cmd The command to send.
     * @param outBuffer The data to send with the command.
     * @param outBufferOffset The offset of the first byte to send.
     * @param outBufferCount The number of bytes to send, if any.
     * @param inBuffer The buffer to fill with returned data.
     * @param inBufferOffset The first byte to fill with returned data.
     * @param inBufferMax The maximum amount of data to receive.
     * @return The number of bytes received.
     */
    private int send(ScsiCommand cmd,
                     byte[] outBuffer,
                     int outBufferOffset,
                     int outBufferCount,
                     byte[] inBuffer,
                     int inBufferOffset,
                     int inBufferMax) {
        return getActiveConnection()
                .send(cmd, outBuffer, outBufferOffset, outBufferCount, inBuffer, inBufferOffset, inBufferMax);
    }

    private <T extends ScsiResponse> T send(Class<T> clazz, ScsiCommand cmd, byte[] buffer, int offset, int count, int expected) {
        return getActiveConnection().send(clazz, cmd, buffer, offset, count, expected);
    }
}
