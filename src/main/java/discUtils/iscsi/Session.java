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

package discUtils.iscsi;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import dotnet4j.io.FileAccess;


/**
 * Represents a connection to a particular Target.
 */
public final class Session implements Closeable {

    private static AtomicInteger nextInitiatorSessionId = new AtomicInteger(new SecureRandom().nextInt());

    private final List<TargetAddress> addresses;

    /**
     * The set of all 'parameters' we've negotiated.
     */
    private final Map<String, String> negotiatedParameters;

    private short nextConnectionId;

    Session(SessionType type, String targetName, List<TargetAddress> addresses) {
        this(type, targetName, null, null, addresses);
    }

    Session(SessionType type, String targetName, String userName, String password, List<TargetAddress> addresses) {
        initiatorSessionId = nextInitiatorSessionId.incrementAndGet();
        this.addresses = addresses;

        sessionType = type;
        this.targetName = targetName;

        setCommandSequenceNumber(1);
        setCurrentTaskTag(1);

        // Default negotiated values...
        maxConnections = 1;
        initialR2T = true;
        immediateData = true;
        maxBurstLength = 262144;
        firstBurstLength = 65536;
        defaultTime2Wait = 0;
        defaultTime2Retain = 60;
        maxOutstandingR2T = 1;
        dataPDUInOrder = true;
        dataSequenceInOrder = true;

        negotiatedParameters = new HashMap<>();

        if (userName == null || userName.isEmpty()) {
            setActiveConnection(new Connection(this,
                                               this.addresses.get(0),
                                               new Authenticator[] {
                                                   new NullAuthenticator()
                                               }));
        } else {
            setActiveConnection(new Connection(this,
                                               this.addresses.get(0),
                                               new Authenticator[] {
                                                   new NullAuthenticator(), new ChapAuthenticator(userName, password)
                                               }));
        }
    }

    private Connection activeConnection;

    Connection getActiveConnection() {
        return activeConnection;
    }

    void setActiveConnection(Connection value) {
        activeConnection = value;
    }

    private int commandSequenceNumber;

    int getCommandSequenceNumber() {
        return commandSequenceNumber;
    }

    void setCommandSequenceNumber(int value) {
        commandSequenceNumber = value;
    }

    private int currentTaskTag;

    int getCurrentTaskTag() {
        return currentTaskTag;
    }

    void setCurrentTaskTag(int value) {
        currentTaskTag = value;
    }

    private int initiatorSessionId;

    int getInitiatorSessionId() {
        return initiatorSessionId;
    }

    private short targetSessionId;

    short getTargetSessionId() {
        return targetSessionId;
    }

    void setTargetSessionId(short value) {
        targetSessionId = value;
    }

    /**
     * Disposes of this instance, closing the session with the Target.
     */
    @Override
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
        return getActiveConnection().enumerateTargets();
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
        TargetInfo targetInfo = new TargetInfo(targetName, addresses);
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
     * @param command The command (a SCSI Command Descriptor block, aka CDB).
     * @param outBuffer buffer of data to send with the command (or
     *            {@code null}).
     * @param outBufferOffset Offset of first byte of data to send with the
     *            command.
     * @param outBufferLength Amount of data to send with the command.
     * @param inBuffer buffer to receive data from the command (or
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
        return ++commandSequenceNumber;
    }

    int nextTaskTag() {
        return ++currentTaskTag;
    }

    short nextConnectionId() {
        return ++nextConnectionId;
    }

    void getParametersToNegotiate(TextBuffer parameters, KeyUsagePhase phase) {
        try {
            for (Field propInfo : getClass().getDeclaredFields()) {
                ProtocolKeyAttribute attr = propInfo.getAnnotation(ProtocolKeyAttribute.class);
                if (attr != null) {
                    Object value = propInfo.get(this);
                    if (ProtocolKeyAttribute.Util.shouldTransmit(attr,
                                                                 value,
                                                                 propInfo.getType(),
                                                                 phase,
                                                                 sessionType == SessionType.Discovery)) {
                        parameters.add(attr.name(), ProtocolKeyAttribute.Util.getValueAsString(value, propInfo.getType()));
                        negotiatedParameters.put(attr.name(), "");
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
                ProtocolKeyAttribute attr = propInfo.getAnnotation(ProtocolKeyAttribute.class);
                if (attr != null) {
                    if (inParameters.get(attr.name()) != null) {
                        Object value = ProtocolKeyAttribute.Util.getValueAsObject(inParameters.get(attr.name()),
                                                                                  propInfo.getType());
                        propInfo.set(this, value);
                        inParameters.remove(attr.name());

                        if (attr.type() == KeyType.Negotiated && !negotiatedParameters.containsKey(attr.name())) {
                            value = propInfo.get(this);
                            outParameters.add(attr.name(),
                                              ProtocolKeyAttribute.Util.getValueAsString(value, propInfo.getType()));
                            negotiatedParameters.put(attr.name(), "");
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
    public String targetName;

    /**
     * Gets the name of the iSCSI initiator seen by the target for this session.
     */
    @ProtocolKeyAttribute(name = "InitiatorName",
                          phase = KeyUsagePhase.SecurityNegotiation,
                          sender = KeySender.Initiator,
                          type = KeyType.Declarative,
                          usedForDiscovery = true)
    public String initiatorName = "iqn.2008-2010-04.discutils.codeplex.com";

    /**
     * Gets the friendly name of the iSCSI target this session is connected to.
     */
    @ProtocolKeyAttribute(name = "TargetAlias",
                          defaultValue = "",
                          phase = KeyUsagePhase.All,
                          sender = KeySender.Target,
                          type = KeyType.Declarative)
    String targetAlias;

    @ProtocolKeyAttribute(name = "SessionType",
                          phase = KeyUsagePhase.SecurityNegotiation,
                          sender = KeySender.Initiator,
                          type = KeyType.Declarative,
                          usedForDiscovery = true)
    SessionType sessionType;

    @ProtocolKeyAttribute(name = "MaxConnections",
                          defaultValue = "1",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    int maxConnections;

    @ProtocolKeyAttribute(name = "InitiatorAlias",
                          defaultValue = "",
                          phase = KeyUsagePhase.All,
                          sender = KeySender.Initiator,
                          type = KeyType.Declarative)
    String initiatorAlias;

    @ProtocolKeyAttribute(name = "TargetPortalGroupTag",
                          phase = KeyUsagePhase.SecurityNegotiation,
                          sender = KeySender.Target,
                          type = KeyType.Declarative)
    int targetPortalGroupTag;

    @ProtocolKeyAttribute(name = "InitialR2T",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    boolean initialR2T;

    @ProtocolKeyAttribute(name = "ImmediateData",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    boolean immediateData;

    @ProtocolKeyAttribute(name = "MaxBurstLength",
                          defaultValue = "262144",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    int maxBurstLength;

    @ProtocolKeyAttribute(name = "FirstBurstLength",
                          defaultValue = "65536",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    int firstBurstLength;

    @ProtocolKeyAttribute(name = "DefaultTime2Wait",
                          defaultValue = "2",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    int defaultTime2Wait;

    @ProtocolKeyAttribute(name = "DefaultTime2Retain",
                          defaultValue = "20",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    int defaultTime2Retain;

    @ProtocolKeyAttribute(name = "MaxOutstandingR2T",
                          defaultValue = "1",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    int maxOutstandingR2T;

    @ProtocolKeyAttribute(name = "DataPDUInOrder",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    boolean dataPDUInOrder;

    @ProtocolKeyAttribute(name = "DataSequenceInOrder",
                          defaultValue = "Yes",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    boolean dataSequenceInOrder;

    @ProtocolKeyAttribute(name = "ErrorRecoveryLevel",
                          defaultValue = "0",
                          phase = KeyUsagePhase.OperationalNegotiation,
                          sender = KeySender.Both,
                          type = KeyType.Negotiated,
                          leadingConnectionOnly = true)
    int errorRecoveryLevel;

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
