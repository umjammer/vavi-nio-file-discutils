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
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dotnet4j.io.MemoryStream;
import dotnet4j.io.NetworkStream;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


class Connection implements Closeable {

    private final Authenticator[] authenticators;

    /**
     * The set of all 'parameters' we've negotiated.
     */
    private final Map<String, String> negotiatedParameters;

    private final Stream stream;

    public Connection(Session session, TargetAddress address, Authenticator[] authenticators) {
        try {
            this.session = session;
            this.authenticators = authenticators;

            Socket client = new Socket(address.getNetworkAddress(), address.getNetworkPort());
            client.setTcpNoDelay(true);
            stream = new NetworkStream(client);

            id = session.nextConnectionId();

            // Default negotiated values
            headerDigest = Digest.None;
            dataDigest = Digest.None;
            maxInitiatorTransmitDataSegmentLength = 131072;
            maxTargetReceiveDataSegmentLength = 8192;

            negotiatedParameters = new HashMap<>();
            negotiateSecurity();
            negotiateFeatures();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private LoginStages currentLoginStage = LoginStages.SecurityNegotiation;

    LoginStages getCurrentLoginStage() {
        return currentLoginStage;
    }

    private int expectedStatusSequenceNumber = 1;

    int getExpectedStatusSequenceNumber() {
        return expectedStatusSequenceNumber;
    }

    private final short id;

    short getId() {
        return id;
    }

    LoginStages getNextLoginStage() {
        return switch (currentLoginStage) {
            case SecurityNegotiation -> LoginStages.LoginOperationalNegotiation;
            default -> LoginStages.FullFeaturePhase;
        };
    }

    private final Session session;

    Session getSession() {
        return session;
    }

    @Override public void close() throws IOException {
        LogoutRequest req = new LogoutRequest(this);
        byte[] packet = req.getBytes(LogoutReason.CloseConnection);
        stream.write(packet, 0, packet.length);
        stream.flush();

        ProtocolDataUnit pdu = readPdu();
        LogoutResponse resp = parseResponse(LogoutResponse.class, pdu);

        if (resp.response != LogoutResponseCode.ClosedSuccessfully) {
            throw new InvalidProtocolException("Target indicated failure during logout: " + resp.response);
        }

        stream.close();
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
    public int send(ScsiCommand cmd, byte[] outBuffer, int outBufferOffset, int outBufferCount, byte[] inBuffer, int inBufferOffset, int inBufferMax) {
        CommandRequest req = new CommandRequest(this, cmd.getTargetLun());

        int toSend = Math.min(Math.min(outBufferCount, session.immediateData ? session.firstBurstLength : 0), maxTargetReceiveDataSegmentLength);
        byte[] packet = req.getBytes(cmd, outBuffer, outBufferOffset, toSend, true, inBufferMax != 0, outBufferCount != 0, outBufferCount != 0 ? outBufferCount : inBufferMax);
        stream.write(packet, 0, packet.length);
        stream.flush();

        int numApproved;
        int numSent = toSend;
        int pktsSent = 0;
        while (numSent < outBufferCount) {
            ProtocolDataUnit pdu = readPdu();

            ReadyToTransferPacket resp = parseResponse(ReadyToTransferPacket.class, pdu);
            numApproved = resp.desiredTransferLength;
            int targetTransferTag = resp.targetTransferTag;

            while (numApproved > 0) {
                toSend = Math.min(Math.min(outBufferCount - numSent, numApproved), maxTargetReceiveDataSegmentLength);

                DataOutPacket pkt = new DataOutPacket(this, cmd.getTargetLun());
                packet = pkt.getBytes(outBuffer, outBufferOffset + numSent, toSend, toSend == numApproved, pktsSent++, numSent, targetTransferTag);
                stream.write(packet, 0, packet.length);
                stream.flush();

                numApproved -= toSend;
                numSent += toSend;
            }
        }

        boolean isFinal = false;
        int numRead = 0;
        while (!isFinal) {
            ProtocolDataUnit pdu = readPdu();

            if (pdu.getOpCode() == OpCode.ScsiResponse) {
                Response resp = parseResponse(Response.class, pdu);

                if (resp.statusPresent && resp.status == ScsiStatus.CheckCondition) {
                    short senseLength = ByteUtil.readBeShort(pdu.getContentData(), 0);
                    byte[] senseData = new byte[senseLength];
                    System.arraycopy(pdu.getContentData(), 2, senseData, 0, senseLength);
                    throw new ScsiCommandException(resp.status, "Target indicated SCSI failure", senseData);
                }
                if (resp.statusPresent && resp.status != ScsiStatus.Good) {
                    throw new ScsiCommandException(resp.status, "Target indicated SCSI failure");
                }

                isFinal = resp.header.finalPdu;
            } else if (pdu.getOpCode() == OpCode.ScsiDataIn) {
                DataInPacket resp = parseResponse(DataInPacket.class, pdu);

                if (resp.statusPresent && resp.status != ScsiStatus.Good) {
                    throw new ScsiCommandException(resp.status, "Target indicated SCSI failure");
                }

                if (resp.readData != null) {
                    System.arraycopy(resp.readData, 0, inBuffer, inBufferOffset + resp.bufferOffset, resp.readData.length);
                    numRead += resp.readData.length;
                }

                isFinal = resp.header.finalPdu;
            }
        }

        session.nextTaskTag();
        session.nextCommandSequenceNumber();

        return numRead;
    }

    public <T extends ScsiResponse> T send(Class<T> clazz, ScsiCommand cmd, byte[] buffer, int offset, int count, int expected) {
        try {
            byte[] tempBuffer = new byte[expected];
            int numRead = send(cmd, buffer, offset, count, tempBuffer, 0, expected);

            T result = clazz.getDeclaredConstructor().newInstance();
            result.readFrom(tempBuffer, 0, numRead);
            return result;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public TargetInfo[] enumerateTargets() {
        TextBuffer parameters = new TextBuffer();
        parameters.add(SendTargetsParameter, "All");

        byte[] paramBuffer = new byte[parameters.getSize()];
        parameters.writeTo(paramBuffer, 0);

        TextRequest req = new TextRequest(this);
        byte[] packet = req.getBytes(0, paramBuffer, 0, paramBuffer.length, true);

        stream.write(packet, 0, packet.length);
        stream.flush();

        ProtocolDataUnit pdu = readPdu();
        TextResponse resp = parseResponse(TextResponse.class, pdu);

        TextBuffer buffer = new TextBuffer();
        if (resp.textData != null) {
            buffer.readFrom(resp.textData, 0, resp.textData.length);
        }

        List<TargetInfo> targets = new ArrayList<>();

        String currentTarget = null;
        List<TargetAddress> currentAddresses = null;
        for (Map.Entry<String, String> line : buffer.getLines().entrySet()) {
            if (currentTarget == null) {
                if (!line.getKey().equals(TargetNameParameter)) {
                    throw new InvalidProtocolException("Unexpected response parameter " + line.getKey() + " expected " + TargetNameParameter);
                }

                currentTarget = line.getValue();
                currentAddresses = new ArrayList<>();
            } else if (line.getKey().equals(TargetNameParameter)) {
                targets.add(new TargetInfo(currentTarget, currentAddresses));
                currentTarget = line.getValue();
                currentAddresses.clear();
            } else if (line.getKey().equals(TargetAddressParameter)) {
                currentAddresses.add(TargetAddress.parse(line.getValue()));
            }
        }

        if (currentTarget != null) {
            targets.add(new TargetInfo(currentTarget, currentAddresses));
        }

        return targets.toArray(new TargetInfo[0]);
    }

    void seenStatusSequenceNumber(int number) {
        if (number != 0 && number != expectedStatusSequenceNumber) {
            throw new InvalidProtocolException("Unexpected status sequence number " + number + ", expected " + expectedStatusSequenceNumber);
        }

        expectedStatusSequenceNumber = number + 1;
    }

    private void negotiateSecurity() {
        currentLoginStage = LoginStages.SecurityNegotiation;

        //
        // Establish the contents of the request
        //
        TextBuffer parameters = new TextBuffer();

        getParametersToNegotiate(parameters, KeyUsagePhase.SecurityNegotiation, session.sessionType);
        session.getParametersToNegotiate(parameters, KeyUsagePhase.SecurityNegotiation);

        StringBuilder authParam = new StringBuilder(authenticators[0].getIdentifier());
        for (int i = 1; i < authenticators.length; ++i) {
            authParam.append(",").append(authenticators[i].getIdentifier());
        }

        parameters.add(AuthMethodParameter, authParam.toString());

        //
        // Send the request...
        //
        byte[] paramBuffer = new byte[parameters.getSize()];
        parameters.writeTo(paramBuffer, 0);

        LoginRequest req = new LoginRequest(this);
        byte[] packet = req.getBytes(paramBuffer, 0, paramBuffer.length, true);
//logger.log(Level.DEBUG, "\n" + StringUtil.getDump(packet));

        stream.write(packet, 0, packet.length);
        stream.flush();

        //
        // Read the response...
        //
        TextBuffer settings = new TextBuffer();

        ProtocolDataUnit pdu = readPdu();
        LoginResponse resp = parseResponse(LoginResponse.class, pdu);

        if (resp.statusCode != LoginStatusCode.Success) {
            throw new LoginException("iSCSI Target indicated login failure: " + resp.statusCode);
        }

        if (resp.continue_) {
            try (MemoryStream ms = new MemoryStream()) {
                ms.write(resp.textData, 0, resp.textData.length);

                while (resp.continue_) {
                    pdu = readPdu();
                    resp = parseResponse(LoginResponse.class, pdu);
                    ms.write(resp.textData, 0, resp.textData.length);
                }

                settings.readFrom(ms.toArray(), 0, (int) ms.getLength());
            }
        } else if (resp.textData != null) {
            settings.readFrom(resp.textData, 0, resp.textData.length);
        }

        Authenticator authenticator = null;
        for (Authenticator value : authenticators) {
            if (settings.get(AuthMethodParameter).equals(value.getIdentifier())) {
                authenticator = value;
                break;
            }
        }

        settings.remove(AuthMethodParameter);
        settings.remove("TargetPortalGroupTag");

        if (authenticator == null) {
            throw new LoginException("iSCSI Target specified an unsupported authentication method: " + settings.get(AuthMethodParameter));
        }

        parameters = new TextBuffer();
        consumeParameters(settings, parameters);

        while (!resp.transit) {
            //
            // Send the request...
            //
            parameters = new TextBuffer();
            authenticator.getParameters(parameters);
            paramBuffer = new byte[parameters.getSize()];
            parameters.writeTo(paramBuffer, 0);

            req = new LoginRequest(this);
            packet = req.getBytes(paramBuffer, 0, paramBuffer.length, true);

            stream.write(packet, 0, packet.length);
            stream.flush();

            //
            // Read the response...
            //
            settings = new TextBuffer();

            pdu = readPdu();
            resp = parseResponse(LoginResponse.class, pdu);

            if (resp.statusCode != LoginStatusCode.Success) {
                throw new LoginException("iSCSI Target indicated login failure: " + resp.statusCode);
            }

            if (resp.textData != null && resp.textData.length != 0) {
                if (resp.continue_) {
                    MemoryStream ms = new MemoryStream();
                    ms.write(resp.textData, 0, resp.textData.length);

                    while (resp.continue_) {
                        pdu = readPdu();
                        resp = parseResponse(LoginResponse.class, pdu);
                        ms.write(resp.textData, 0, resp.textData.length);
                    }

                    settings.readFrom(ms.toArray(), 0, (int) ms.getLength());
                } else {
                    settings.readFrom(resp.textData, 0, resp.textData.length);
                }

                authenticator.setParameters(settings);
            }
        }

        if (resp.nextStage != getNextLoginStage()) {
            throw new LoginException("iSCSI Target wants to transition to a different login stage: " + resp.nextStage + " (expected: " + getNextLoginStage() + ")");
        }

        currentLoginStage = resp.nextStage;
    }

    private void negotiateFeatures() {
        //
        // Send the request...
        //
        TextBuffer parameters = new TextBuffer();
        getParametersToNegotiate(parameters, KeyUsagePhase.OperationalNegotiation, session.sessionType);
        session.getParametersToNegotiate(parameters, KeyUsagePhase.OperationalNegotiation);

        byte[] paramBuffer = new byte[parameters.getSize()];
        parameters.writeTo(paramBuffer, 0);

        LoginRequest req = new LoginRequest(this);
        byte[] packet = req.getBytes(paramBuffer, 0, paramBuffer.length, true);

        stream.write(packet, 0, packet.length);
        stream.flush();

        //
        // Read the response...
        //
        TextBuffer settings = new TextBuffer();

        ProtocolDataUnit pdu = readPdu();
        LoginResponse resp = parseResponse(LoginResponse.class, pdu);

        if (resp.statusCode != LoginStatusCode.Success) {
            throw new LoginException("iSCSI Target indicated login failure: " + resp.statusCode);
        }

        if (resp.continue_) {
            try (MemoryStream ms = new MemoryStream()) {
                ms.write(resp.textData, 0, resp.textData.length);

                while (resp.continue_) {
                    pdu = readPdu();
                    resp = parseResponse(LoginResponse.class, pdu);
                    ms.write(resp.textData, 0, resp.textData.length);
                }

                settings.readFrom(ms.toArray(), 0, (int) ms.getLength());
            }
        } else if (resp.textData != null) {
            settings.readFrom(resp.textData, 0, resp.textData.length);
        }

        parameters = new TextBuffer();
        consumeParameters(settings, parameters);

        while (!resp.transit || parameters.getCount() != 0) {
            paramBuffer = new byte[parameters.getSize()];
            parameters.writeTo(paramBuffer, 0);

            req = new LoginRequest(this);
            packet = req.getBytes(paramBuffer, 0, paramBuffer.length, true);

            stream.write(packet, 0, packet.length);
            stream.flush();

            //
            // Read the response...
            //
            settings = new TextBuffer();

            pdu = readPdu();
            resp = parseResponse(LoginResponse.class, pdu);

            if (resp.statusCode != LoginStatusCode.Success) {
                throw new LoginException("iSCSI Target indicated login failure: " + resp.statusCode);
            }

            parameters = new TextBuffer();

            if (resp.textData != null) {
                if (resp.continue_) {
                    MemoryStream ms = new MemoryStream();
                    ms.write(resp.textData, 0, resp.textData.length);

                    while (resp.continue_) {
                        pdu = readPdu();
                        resp = parseResponse(LoginResponse.class, pdu);
                        ms.write(resp.textData, 0, resp.textData.length);
                    }

                    settings.readFrom(ms.toArray(), 0, (int) ms.getLength());
                } else {
                    settings.readFrom(resp.textData, 0, resp.textData.length);
                }

                consumeParameters(settings, parameters);
            }
        }

        if (resp.nextStage != getNextLoginStage()) {
            throw new LoginException("iSCSI Target wants to transition to a different login stage: " + resp.nextStage + " (expected: " + getNextLoginStage() + ")");
        }

        currentLoginStage = resp.nextStage;
    }

    private ProtocolDataUnit readPdu() {
        ProtocolDataUnit pdu = ProtocolDataUnit.readFrom(stream, headerDigest != Digest.None, dataDigest != Digest.None);

        if (pdu.getOpCode() == OpCode.Reject) {
            RejectPacket pkt = new RejectPacket();
            pkt.parse(pdu);

            throw new IscsiException("Target sent reject packet, reason " + pkt.reason);
        }

        return pdu;
    }

    private void getParametersToNegotiate(TextBuffer parameters, KeyUsagePhase phase, SessionType sessionType) {
        try {
            for (Field propInfo : getClass().getDeclaredFields()) {
                ProtocolKeyAttribute attr = propInfo.getAnnotation(ProtocolKeyAttribute.class);
                if (attr != null) {
                    Object value = propInfo.get(this);

                    if (ProtocolKeyAttribute.Util.shouldTransmit(attr, value, propInfo.getType(), phase, sessionType == SessionType.Discovery)) {
                        parameters.add(attr.name(), ProtocolKeyAttribute.Util.getValueAsString(value, propInfo.getType()));
                        negotiatedParameters.put(attr.name(), "");
                    }
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private void consumeParameters(TextBuffer inParameters, TextBuffer outParameters) {
        try {
            for (Field propInfo : getClass().getDeclaredFields()) {
                ProtocolKeyAttribute attr = propInfo.getAnnotation(ProtocolKeyAttribute.class);
                if (attr != null && attr.sender() == KeySender.Target) {
                    if (inParameters.get(attr.name()) != null) {
                        Object value = ProtocolKeyAttribute.Util.getValueAsObject(inParameters.get(attr.name()), propInfo.getType());

                        propInfo.set(this, value);
                        inParameters.remove(attr.name());

                        if (attr.type() == KeyType.Negotiated && !negotiatedParameters.containsKey(attr.name())) {
                            value = propInfo.get(this);
                            outParameters.add(attr.name(), ProtocolKeyAttribute.Util.getValueAsString(value, propInfo.getType()));
                            negotiatedParameters.put(attr.name(), "");
                        }
                    }
                }
            }

            session.consumeParameters(inParameters, outParameters);

            for (Map.Entry<String, String> param : inParameters.getLines().entrySet()) {
                outParameters.add(param.getKey(), "NotUnderstood");
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseResponse> T parseResponse(Class<T> clazz, ProtocolDataUnit pdu) {
        BaseResponse resp = switch (pdu.getOpCode()) {
            case LoginResponse -> new LoginResponse();
            case LogoutResponse -> new LogoutResponse();
            case ReadyToTransfer -> new ReadyToTransferPacket();
            case Reject -> new RejectPacket();
            case ScsiDataIn -> new DataInPacket();
            case ScsiResponse -> new Response();
            case TextResponse -> new TextResponse();
            default -> throw new InvalidProtocolException("Unrecognized response opcode: " + pdu.getOpCode());
        };

        resp.parse(pdu);
        if (resp.statusPresent) {
            seenStatusSequenceNumber(resp.statusSequenceNumber);
        }

        if (!clazz.isInstance(resp)) {
            throw new InvalidProtocolException("Unexpected response, expected " + clazz.getName() + ", got " + resp.getClass());
        }

        return (T) resp;
    }

    private static final String InitiatorNameParameter = "InitiatorName";

    private static final String SessionTypeParameter = "SessionType";

    private static final String AuthMethodParameter = "AuthMethod";

    private static final String HeaderDigestParameter = "HeaderDigest";

    private static final String DataDigestParameter = "DataDigest";

    private static final String MaxRecvDataSegmentLengthParameter = "MaxRecvDataSegmentLength";

    private static final String DefaultTime2WaitParameter = "DefaultTime2Wait";

    private static final String DefaultTime2RetainParameter = "DefaultTime2Retain";

    private static final String SendTargetsParameter = "SendTargets";

    private static final String TargetNameParameter = "TargetName";

    private static final String TargetAddressParameter = "TargetAddress";

    private static final String NoneValue = "None";

    private static final String ChapValue = "CHAP";

    @ProtocolKeyAttribute(name = "HeaderDigest", defaultValue = "None", phase = KeyUsagePhase.OperationalNegotiation, sender = KeySender.Both, type = KeyType.Negotiated, usedForDiscovery = true)
    public Digest headerDigest;

    @ProtocolKeyAttribute(name = "DataDigest", defaultValue = "None", phase = KeyUsagePhase.OperationalNegotiation, sender = KeySender.Both, type = KeyType.Negotiated, usedForDiscovery = true)
    public Digest dataDigest;

    @ProtocolKeyAttribute(name = "MaxRecvDataSegmentLength", defaultValue = "8192", phase = KeyUsagePhase.OperationalNegotiation, sender = KeySender.Initiator, type = KeyType.Declarative)
    int maxInitiatorTransmitDataSegmentLength;

    @ProtocolKeyAttribute(name = "MaxRecvDataSegmentLength", defaultValue = "8192", phase = KeyUsagePhase.OperationalNegotiation, sender = KeySender.Target, type = KeyType.Declarative)
    int maxTargetReceiveDataSegmentLength;
}
