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

package discUtils.powerShell.virtualDiskProvider;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import discUtils.core.coreCompat.IContentReader;
import discUtils.core.coreCompat.IContentWriter;
import discUtils.powerShell.conpat.ErrorRecord;
import dotnet4j.io.IOException;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;
import dotnet4j.io.StreamWriter;


public final class FileContentReaderWriter implements IContentWriter, IContentReader {

    private Provider _provider;

    private Stream _contentStream;

    private ContentEncoding _encoding = ContentEncoding.Unknown;

    private StreamReader _reader;

    private StreamWriter _writer;

    public FileContentReaderWriter(Provider provider, Stream contentStream, ContentParameters dynParams) {
        _provider = provider;
        _contentStream = contentStream;
        _contentStream.setPosition(0);
        if (dynParams != null) {
            _encoding = dynParams.getEncoding();
        }
    }

    public void close() {
        if (_writer != null) {
            _writer.flush();
        }

        try {
            _contentStream.close();
        } catch (Exception e) {
            _provider.writeError(new ErrorRecord(new IOException("Failure using virtual disk", e),
                                                 "CloseFailed",
                                                 ErrorCategory.WriteError,
                                                 null));
        }
    }

    public void seek(long offset, SeekOrigin origin) {
        _contentStream.seek(offset, origin);
    }

    public List<?> read(long readCount) {
        try {
            if (_encoding == ContentEncoding.Byte) {
                if (readCount <= 0) {
                    readCount = Long.MAX_VALUE;
                }

                int maxToRead = (int) Math.min(Math.min(readCount, _contentStream.getLength() - _contentStream.getPosition()),
                                               Integer.MAX_VALUE);
                byte[] fileContent = new byte[maxToRead];
                int numRead = _contentStream.read(fileContent, 0, maxToRead);
                Object[] result = new Object[numRead];
                for (int i = 0; i < numRead; ++i) {
                    result[i] = fileContent[i];
                }
                return Arrays.asList(result);
            } else {
                List<Object> result = new ArrayList<>();
                if (_reader == null) {
                    if (_encoding == ContentEncoding.Unknown) {
                        _reader = new StreamReader(_contentStream, StandardCharsets.US_ASCII, true);
                    } else {
                        _reader = new StreamReader(_contentStream, getEncoding(StandardCharsets.US_ASCII));
                    }
                }

                while ((result.size() < readCount || readCount <= 0) && !_reader.isEndOfStream()) {
                    result.add(_reader.readLine());
                }
                return result;
            }
        } catch (Exception e) {
            _provider.writeError(new ErrorRecord(new IOException("Failure reading from virtual disk" + e, e),
                                                 "ReadFailed",
                                                 ErrorCategory.readError,
                                                 null));
            return null;
        }

    }

    public List<?> write(List<?> content) {
        try {
            if (content == null || content.size() == 0) {
                return content;
            }

            if (content.get(0).getClass() == byte.class) {
                byte[] buffer = new byte[content.size()];
                for (int i = 0; i < buffer.length; ++i) {
                    buffer[i] = (byte) content.get(i);
                }
                _contentStream.write(buffer, 0, buffer.length);
                return content;
            } else if ((content.get(0) instanceof String ? (String) content.get(0) : (String) null) != null) {
                if (_writer == null) {
                    String initialContent = (String) content.get(0);
                    boolean foundExtended = false;
                    int toInspect = Math.min(20, initialContent.length());
                    for (int i = 0; i < toInspect; ++i) {
                        if (((short) initialContent.charAt(i)) > 127) {
                            foundExtended = true;
                            break;
                            break;
                        }
                    }
                    _writer = new StreamWriter(_contentStream,
                                               getEncoding(foundExtended ? Charset.forName("Unicode")
                                                                         : StandardCharsets.US_ASCII));
                }

                String lastLine = null;
                for (Object s_ : content) {
                    String s = s_.toString();
                    _writer.println(s);
                    lastLine = s;
                }
                _writer.flush();
                return content;
            } else {
                return null;
            }
        } catch (Exception e) {
            _provider.writeError(new ErrorRecord(new IOException("Failure writing to virtual disk", e),
                                                 "WriteFailed",
                                                 ErrorCategory.WriteError,
                                                 null));
            return null;
        }

    }

    protected void finalize() throws Throwable {
        if (_writer != null) {
            _writer.close();
        }

        if (_reader != null) {
            _reader.close();
        }

        if (_contentStream != null) {
            _contentStream.close();
        }
    }

    /**
     * @see "https://sourceforge.net/projects/jutf7/"
     * @see "https://ja.osdn.net/projects/sfnet_jutf7/"
     */
    private Charset getEncoding(Charset defEncoding) {
        switch (_encoding) {
        case UTF16:
            return StandardCharsets.UTF_16;
        case UTF8:
            return StandardCharsets.UTF_8;
        case UTF7:
            return Charset.forName("UTF-7");
        case Unicode:
            return Charset.forName("Unicode");
        case Ascii:
            return StandardCharsets.US_ASCII;
        default:
            return defEncoding;
        }
    }
}
