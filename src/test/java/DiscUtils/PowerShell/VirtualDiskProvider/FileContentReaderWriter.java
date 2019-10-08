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

package DiscUtils.PowerShell.VirtualDiskProvider;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.CoreCompat.IContentReader;
import DiscUtils.Core.CoreCompat.IContentWriter;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;
import moe.yo3explorer.dotnetio4j.StreamReader;
import moe.yo3explorer.dotnetio4j.StreamWriter;


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
                        _reader = new StreamReader(_contentStream, Charset.forName("ASCII"), true);
                    } else {
                        _reader = new StreamReader(_contentStream, getEncoding(Charset.forName("ASCII")));
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
                        }
                    }
                    _writer = new StreamWriter(_contentStream,
                                               getEncoding(foundExtended ? Charset.forName("Unicode")
                                                                         : Charset.forName("ASCII")));
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

    private Charset getEncoding(Charset defEncoding) {
        switch (_encoding) {
        case BigEndianUnicode:
            return Charset.forName("BigEndianUnicode");
        case UTF8:
            return Charset.forName("UTF8");
        case UTF7:
            return Charset.forName("UTF7");
        case Unicode:
            return Charset.forName("Unicode");
        case Ascii:
            return Charset.forName("ASCII");
        default:
            return defEncoding;
        }
    }
}
