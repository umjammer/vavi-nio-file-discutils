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

package DiscUtils.Net.Dns;

import java.util.ArrayList;
import java.util.List;


public class Message {
    public Message() {
        __Questions = new ArrayList<>();
        __Answers = new ArrayList<>();
        __AuthorityRecords = new ArrayList<>();
        __AdditionalRecords = new ArrayList<>();
    }

    private List<ResourceRecord> __AdditionalRecords;

    public List<ResourceRecord> getAdditionalRecords() {
        return __AdditionalRecords;
    }

    private List<ResourceRecord> __Answers;

    public List<ResourceRecord> getAnswers() {
        return __Answers;
    }

    private List<ResourceRecord> __AuthorityRecords;

    public List<ResourceRecord> getAuthorityRecords() {
        return __AuthorityRecords;
    }

    private MessageFlags __Flags;

    public MessageFlags getFlags() {
        return __Flags;
    }

    public void setFlags(MessageFlags value) {
        __Flags = value;
    }

    private List<Question> __Questions;

    public List<Question> getQuestions() {
        return __Questions;
    }

    private short __TransactionId;

    public short getTransactionId() {
        return __TransactionId;
    }

    public void setTransactionId(short value) {
        __TransactionId = value;
    }

    public static Message read(PacketReader reader) {
        Message result = new Message();
        result.setTransactionId(reader.readUShort());
        result.setFlags(new MessageFlags(reader.readUShort()));
        short questions = reader.readUShort();
        short answers = reader.readUShort();
        short authorityRecords = reader.readUShort();
        short additionalRecords = reader.readUShort();
        for (int i = 0; i < questions; ++i) {
            result.getQuestions().add(Question.readFrom(reader));
        }
        for (int i = 0; i < answers; ++i) {
            result.getAnswers().add(ResourceRecord.readFrom(reader));
        }
        for (int i = 0; i < authorityRecords; ++i) {
            result.getAuthorityRecords().add(ResourceRecord.readFrom(reader));
        }
        for (int i = 0; i < additionalRecords; ++i) {
            result.getAdditionalRecords().add(ResourceRecord.readFrom(reader));
        }
        return result;
    }

    public void writeTo(PacketWriter writer) {
        writer.write(getTransactionId());
        writer.write(getFlags().getValue());
        writer.write((short) getQuestions().size());
        writer.write((byte) 0);
        writer.write((byte) 0);
        writer.write((byte) 0);
        for (Question question : getQuestions()) {
            question.writeTo(writer);
        }
    }
}
