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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


final class Transaction implements Closeable {

    public Transaction() {
        this(null, RecordType.None);
    }

    /**
     * @param name the question this transaction asked, so a response that does not
     *             echo our transaction id (a multicast mDNS response always carries
     *             id 0) can still be matched against it
     * @param type the record type asked for
     */
    public Transaction(String name, RecordType type) {
        this.name = name;
        this.type = type;
        answers = new ArrayList<>();
        completeEvent = new CountDownLatch(1);
    }

    private final String name;

    public String getName() {
        return name;
    }

    private final RecordType type;

    public RecordType getType() {
        return type;
    }

    /** @return whether the record answers the question this transaction asked */
    public boolean matches(ResourceRecord record) {
        return type == record.getRecordType() && name != null && name.equalsIgnoreCase(record.getName());
    }

    private final List<ResourceRecord> answers;

    public List<ResourceRecord> getAnswers() {
        return answers;
    }

    private CountDownLatch completeEvent;

    public CountDownLatch getCompleteEvent() {
        return completeEvent;
    }

    public void setCompleteEvent(CountDownLatch value) {
        completeEvent = value;
    }

    @Override
    public void close() throws IOException {
        if (completeEvent != null) {
            completeEvent.countDown();
            completeEvent = null;
        }
    }
}
