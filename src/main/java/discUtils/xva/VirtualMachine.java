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

package discUtils.xva;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import discUtils.core.archives.TarFile;
import discUtils.streams.util.Ownership;
import dotnet4j.io.Stream;
import dotnet4j.io.compat.StreamInputStream;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 * Class representing the virtual machine stored in a Xen Virtual Appliance
 * (XVA) file. XVA is a VM archive, not just a disk archive. It can contain
 * multiple disk images. This class provides access to all of the disk images
 * within the XVA file.
 */
public final class VirtualMachine implements Closeable {

    private static final String FindVDIsExpression = "/value/struct/member[child::name='objects']/value/array/data/value/struct[child::member/value='VDI']";

    private static final String GetDiskId = "member[child::name='id']/value";

    private static final String GetDiskUuid = "member[child::name='snapshot']/value/struct/member[child::name='uuid']/value";

    private static final String GetDiskNameLabel = "member[child::name='snapshot']/value/struct/member[child::name='name_label']/value";

    private static final String GetDiskCapacity = "member[child::name='snapshot']/value/struct/member[child::name='virtual_size']/value";

    private final Ownership ownership;

    private Stream fileStream;

    /**
     * Initializes a new instance of the VirtualMachine class.
     *
     * @param fileStream The stream containing the .XVA file. Ownership of the
     *            stream is not transfered.
     */
    public VirtualMachine(Stream fileStream) {
        this(fileStream, Ownership.None);
    }

    /**
     * Initializes a new instance of the VirtualMachine class.
     *
     * @param fileStream The stream containing the .XVA file.
     * @param ownership Whether to transfer ownership of {@code fileStream} to
     *            the new instance.
     */
    public VirtualMachine(Stream fileStream, Ownership ownership) {
        this.fileStream = fileStream;
        this.ownership = ownership;
        this.fileStream.position(0);
        archive = new TarFile(fileStream);
    }

    private TarFile archive;

    public TarFile getArchive() {
        return archive;
    }

    /**
     * Gets the disks in this XVA.
     *
     * @return An enumeration of disks.
     */
    public List<Disk> getDisks() {
        List<Disk> result = new ArrayList<>();
        try (Stream docStream = getArchive().openFile("ova.xml")) {
            InputSource ovaDoc = new InputSource(new StreamInputStream(docStream));
            XPath nav = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) nav.evaluate(FindVDIsExpression, ovaDoc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Node idNode = (Node) nav.evaluate(GetDiskId, node, XPathConstants.NODE);

                // Skip disks which are only referenced, not present
                if (getArchive().dirExists(idNode.getTextContent())) {
                    Node uuidNode = (Node) nav.evaluate(GetDiskUuid, node, XPathConstants.NODE);
                    Node nameLabelNode = (Node) nav.evaluate(GetDiskNameLabel, node, XPathConstants.NODE);
                    long capacity = Long
                            .parseLong(((Node) nav.evaluate(GetDiskCapacity, node, XPathConstants.NODE)).getTextContent());
                    result.add(new Disk(this, uuidNode.toString(), nameLabelNode.toString(), idNode.getTextContent(), capacity));
                }
            }
            return result;
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Disposes of this object, freeing any owned resources.
     */
    @Override public void close() throws IOException {
        if (ownership == Ownership.Dispose && fileStream != null) {
            fileStream.close();
            fileStream = null;
        }
    }
}
