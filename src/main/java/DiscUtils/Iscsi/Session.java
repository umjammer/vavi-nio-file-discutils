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

    Session(SessionType type, String targetName, List<TargetAddress> addresses) {
        this(type, targetName, null, null, addresses);
    }

    Session(SessionType type, String targetName, String userName, String password, List<TargetAddress> addresses) {
        _initiatorSessionId = _nextInitiatorSessionId.incrementAndGet();
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

    private Connection _activeConnection;

    Connection getActiveConnection() {
        return _activeConnection;
    }

    void setActiveConnection(Connection value) {
        _activeConnection = value;
    }

    private int _commandSequenceNumber;

    int getCommandSequenceNumber() {
        return _commandSequenceNumber;
    }

    void setCommandSequenceNumber(int value) {
        _commandSequenceNumber = value;
    }

    private int _currentTaskTag;

    int getCurrentTaskTag() {
        return _currentTaskTag;
    }

    void setCurrentTaskTag(int value) {
        _currentTaskTag = value;
    }

    private int _initiatorSessionId;

    int getInitiatorSessionId() {
        return _initiatorSessionId;
    }

    private short _targetSessionId;

    short getTargetSessionId() {
        return _targetSessionId;
    }

    void setTargetSessionId(short value) {
        _targetSessionId = value;
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
     *         just returns details of the connected Target.
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
        ScsiReportLunsResponse resp = send(ScsiReportLunsResponse.class,
                                           cmd,
                                           null,
                                           0,
                                           0,
                                           ScsiReportLunsCommand.InitialResponseSize);
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
        ScsiInquiryStandardResponse resp = send(ScsiInquiryStandardResponse.class,
                                                cmd,
                                                null,
                                                0,
                                                0,
                                                ScsiInquiryCommand.InitialResponseDataLength);
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
        ScsiReadCapacityResponse resp = send(ScsiReadCapacityResponse.class,
                                             cmd,
                                             null,
                                             0,
                                             0,
                                             ScsiReadCapacityCommand.ResponseDataLength);
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
     * This method permits the caller to send raw SCSI commands to a LUN.The
     * command .
     *
     * @param lun The target LUN for the command.
     * @param command The command (a SCSI Command Descriptor Block, aka CDB).
     * @param outBuffer Buffer of data to send with the command (or
     *            {@code null}).
     * @param outBufferOffset Offset of first byte of data to send with the
     *            command.
     * @param outBufferLength Amount of data to send with the command.
     * @param inBuffer Buffer to receive data from the command (or
     *            {@code null}).
     * @param inBufferOffset Offset of the first byte position to fill with
     *            received data.
     * @param inBufferLength The expected amount of data to receive.
     * @return The number of bytes of data received.
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

    int nextCommandSequenceNumber() {
        return ++_commandSequenceNumber;
    }

    int nextTaskTag() {
        return ++_currentTaskTag;
    }

    short nextConnectionId() {
        return ++_nextConnectionId;
    }

    void getParametersToNegotiate(TextBuffer parameters, KeyUsagePhase phase) {
        try {
            for (Field propInfo : getClass().getDeclaredFields()) {
                ProtocolKeyAttribute attr = ReflectionHelper.getCustomAttribute(propInfo, ProtocolKeyAttribute.class);
                if (attr != null) {
                    Object value = propInfo.get(this);
                    if (ProtocolKeyAttribute.Util.shouldTransmit(attr,
                                                                 value,
                                                                 propInfo.getType(),
                                                                 phase,
                                                                 getSessionType() == SessionType.Discovery)) {
                        parameters.add(attr.name(), ProtocolKeyAttribute.Util.getValueAsString(value, propInfo.getType()));
                        _negotiatedParameters.put(attr.name(), "");
                    }
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    void consumeParameters(TextBuffer inParameters, TextBuffer outParameters) {
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
                            outParameters.add(attr.name(),
                                              ProtocolKeyAttribute.Util.getValueAsString(value, propInfo.getType()));
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
    private String _targetName;

    public String getTargetName() {
        return _targetName;
    }

    public void setTargetName(String value) {
        _targetName = value;
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
    private String _targetAlias;

    public String getTargetAlias() {
        return _targetAlias;
    }

    public void setTargetAlias(String value) {
        _targetAlias = value;
    }

    @ProtocolKeyAttribute(name = "SessionType",
                          phase = KeyUsagePhase.SecurityNegotiation,
                          sender = KeySender.Initiator,
                          type = KeyType.Declarative,
                          usedForDiscovery = true)
    private SessionType _sessionType = SessionType.Discovery;

    SessionType getSessionType() {
        return _sessionType;
    }

    void setSessionType(SessionType value) {
        _sessionType = value;
    }

    @ProtocolKeyAttribute(name = "MaxConnections",
                          defaultValue = "1",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int _maxConnections;

    int getMaxConnections() {
        return _maxConnections;
    }

    void setMaxConnections(int value) {
        _maxConnections = value;
    }

    @ProtocolKeyAttribute(name = "InitiatorAlias",
                          defaultValue = "",
                          phase = KeyUsagePhase.All,
                          sender = KeySender.Initiator,
                          type = KeyType.Declarative)
    private String _initiatorAlias;

    String getInitiatorAlias() {
        return _initiatorAlias;
    }

    void setInitiatorAlias(String value) {
        _initiatorAlias = value;
    }

    @ProtocolKeyAttribute(name = "TargetPortalGroupTag",
                          phase = KeyUsagePhase.SecurityNegotiation,
                          sender = KeySender.Target,
                          type = KeyType.Declarative)
    private int _targetPortalGroupTag;

    int getTargetPortalGroupTag() {
        return _targetPortalGroupTag;
    }

    void setTargetPortalGroupTag(int value) {
        _targetPortalGroupTag = value;
    }

    @ProtocolKeyAttribute(name = "InitialR2T",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private boolean _initialR2T;

    boolean getInitialR2T() {
        return _initialR2T;
    }

    void setInitialR2T(boolean value) {
        _initialR2T = value;
    }

    @ProtocolKeyAttribute(name = "ImmediateData",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private boolean _immediateData;

    boolean getImmediateData() {
        return _immediateData;
    }

    void setImmediateData(boolean value) {
        _immediateData = value;
    }

    @ProtocolKeyAttribute(name = "MaxBurstLength",
                          defaultValue = "262144",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int _maxBurstLength;

    int getMaxBurstLength() {
        return _maxBurstLength;
    }

    void setMaxBurstLength(int value) {
        _maxBurstLength = value;
    }

    @ProtocolKeyAttribute(name = "FirstBurstLength",
                          defaultValue = "65536",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int _firstBurstLength;

    int getFirstBurstLength() {
        return _firstBurstLength;
    }

    void setFirstBurstLength(int value) {
        _firstBurstLength = value;
    }

    @ProtocolKeyAttribute(name = "DefaultTime2Wait",
                          defaultValue = "2",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int _defaultTime2Wait;

    int getDefaultTime2Wait() {
        return _defaultTime2Wait;
    }

    void setDefaultTime2Wait(int value) {
        _defaultTime2Wait = value;
    }

    @ProtocolKeyAttribute(name = "DefaultTime2Retain",
                          defaultValue = "20",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int _defaultTime2Retain;

    int getDefaultTime2Retain() {
        return _defaultTime2Retain;
    }

    void setDefaultTime2Retain(int value) {
        _defaultTime2Retain = value;
    }

    @ProtocolKeyAttribute(name = "MaxOutstandingR2T",
                          defaultValue = "1",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int __MaxOutstandingR2T;

    int getMaxOutstandingR2T() {
        return __MaxOutstandingR2T;
    }

    void setMaxOutstandingR2T(int value) {
        __MaxOutstandingR2T = value;
    }

    @ProtocolKeyAttribute(name = "DataPDUInOrder",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private boolean _dataPDUInOrder;

    boolean getDataPDUInOrder() {
        return _dataPDUInOrder;
    }

    void setDataPDUInOrder(boolean value) {
        _dataPDUInOrder = value;
    }

    @ProtocolKeyAttribute(name = "DataSequenceInOrder",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private boolean _dataSequenceInOrder;

    boolean getDataSequenceInOrder() {
        return _dataSequenceInOrder;
    }

    void setDataSequenceInOrder(boolean value) {
        _dataSequenceInOrder = value;
    }

    @ProtocolKeyAttribute(name = "ErrorRecoveryLevel",
                          defaultValue = "0",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    private int _errorRecoveryLevel;

    int getErrorRecoveryLevel() {
        return _errorRecoveryLevel;
    }

    void setErrorRecoveryLevel(int value) {
        _errorRecoveryLevel = value;
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

    private <T extends ScsiResponse> T send(Class<T> clazz,
                                            ScsiCommand cmd,
                                            byte[] buffer,
                                            int offset,
                                            int count,
                                            int expected) {
        return getActiveConnection().send(clazz, cmd, buffer, offset, count, expected);
    }
}
