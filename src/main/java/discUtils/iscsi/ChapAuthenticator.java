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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class ChapAuthenticator extends Authenticator {

    private int algorithm;

    private byte[] challenge;

    private byte identifier;

    private final String name;

    private final String password;

    private State state;

    public ChapAuthenticator(String name, String password) {
        this.name = name;
        this.password = password;
        state = State.SendAlgorithm;
    }

    @Override public String getIdentifier() {
        return "CHAP";
    }

    @Override public boolean getParameters(TextBuffer textBuffer) {
        switch (state) {
        case SendAlgorithm:
            textBuffer.add("CHAP_A", "5");
            state = State.ReceiveChallenge;
            return false;
        case SendResponse:
            textBuffer.add("CHAP_N", name);
            textBuffer.add("CHAP_R", calcResponse());
            state = State.Finished;
            return true;
        default:
            throw new UnsupportedOperationException("Unknown authentication state: " + state);
        }
    }

    @Override public void setParameters(TextBuffer textBuffer) {
        switch (state) {
        case ReceiveChallenge:
            algorithm = Integer.parseInt(textBuffer.get("CHAP_A"));
            identifier = Byte.parseByte(textBuffer.get("CHAP_I"));
            challenge = parseByteString(textBuffer.get("CHAP_C"));
            state = State.SendResponse;
            if (algorithm != 0x5) {
                throw new LoginException("Unexpected CHAP authentication algorithm: " + algorithm);
            }
            return;
        default:
            throw new UnsupportedOperationException("Unknown authentication state: " + state);

        }
    }

    private static byte[] parseByteString(String p) {
        if (!p.startsWith("0x")) {
            throw new IllegalArgumentException("Invalid value in CHAP exchange");
        }

        byte[] data = new byte[(p.length() - 2) / 2];
        for (int i = 0; i < data.length; ++i) {
            data[i] = Byte.parseByte(p.substring(2 + i * 2, 2 + i * 2 + 2), 16);
        }
        return data;
    }

    private String calcResponse() {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] toHash = new byte[1 + password.length() + challenge.length];
            toHash[0] = identifier;
            byte[] bytes = password.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(bytes, 0, toHash, 1, bytes.length);
            System.arraycopy(challenge, 0, toHash, password.length() + 1, challenge.length);
            byte[] hash = md5.digest(toHash);
            StringBuilder result = new StringBuilder("0x");
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private enum State {
        SendAlgorithm,
        ReceiveChallenge,
        SendResponse,
        Finished
    }
}
