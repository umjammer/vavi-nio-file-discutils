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

public final class Question {
    private RecordClass __Class = RecordClass.None;

    public RecordClass getClass_() {
        return __Class;
    }

    public void setClass(RecordClass value) {
        __Class = value;
    }

    private String __Name;

    public String getName() {
        return __Name;
    }

    public void setName(String value) {
        __Name = value;
    }

    private RecordType __Type = RecordType.None;

    public RecordType getType() {
        return __Type;
    }

    public void setType(RecordType value) {
        __Type = value;
    }

    public static Question readFrom(PacketReader reader) {
        Question question = new Question();
        question.setName(reader.readName());
        question.setType(RecordType.valueOf(reader.readUShort()));
        question.setClass(RecordClass.valueOf(reader.readUShort()));
        return question;
    }

    public void writeTo(PacketWriter writer) {
        writer.writeName(getName());
        writer.write((short) getType().ordinal());
        writer.write((short) getClass_().ordinal());
    }
}
