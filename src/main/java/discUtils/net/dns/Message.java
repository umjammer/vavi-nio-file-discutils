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

package discUtils.net.dns;

import java.util.ArrayList;
import java.util.List;


public class Message {

    public Message() {
        questions = new ArrayList<>();
        answers = new ArrayList<>();
        authorityRecords = new ArrayList<>();
        additionalRecords = new ArrayList<>();
    }

    private List<ResourceRecord> additionalRecords;

    public List<ResourceRecord> getAdditionalRecords() {
        return additionalRecords;
    }

    private List<ResourceRecord> answers;

    public List<ResourceRecord> getAnswers() {
        return answers;
    }

    private List<ResourceRecord> authorityRecords;

    public List<ResourceRecord> getAuthorityRecords() {
        return authorityRecords;
    }

    private MessageFlags flags;

    public MessageFlags getFlags() {
        return flags;
    }

    public void setFlags(MessageFlags value) {
        flags = value;
    }

    private List<Question> questions;

    public List<Question> getQuestions() {
        return questions;
    }

    private short transactionId;

    public short getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(short value) {
        transactionId = value;
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
            result.questions.add(Question.readFrom(reader));
        }
        for (int i = 0; i < answers; ++i) {
            result.answers.add(ResourceRecord.readFrom(reader));
        }
        for (int i = 0; i < authorityRecords; ++i) {
            result.authorityRecords.add(ResourceRecord.readFrom(reader));
        }
        for (int i = 0; i < additionalRecords; ++i) {
            result.additionalRecords.add(ResourceRecord.readFrom(reader));
        }
        return result;
    }

    public void writeTo(PacketWriter writer) {
        writer.write(transactionId);
        writer.write(flags.getValue());
        writer.write((short) questions.size());
        writer.write((byte) 0);
        writer.write((byte) 0);
        writer.write((byte) 0);
        for (Question question : questions) {
            question.writeTo(writer);
        }
    }
}
