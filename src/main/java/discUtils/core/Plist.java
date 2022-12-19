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

package discUtils.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dotnet4j.io.Stream;
import dotnet4j.io.compat.StreamInputStream;
import dotnet4j.io.compat.StreamOutputStream;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class Plist {

    private static DocumentBuilderFactory dbf;

    static {
        dbf = DocumentBuilderFactory.newInstance();
    }

    public static Map<String, Object> parse(Stream stream) {
        try {
            // DTD processing is disabled on anything but .NET 2.0, so this must be set to
            // Ignore.
            // See https://msdn.microsoft.com/en-us/magazine/ee335713.aspx for additional information.
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            InputSource reader = new InputSource(new StreamInputStream(stream));
            Document xmlDoc = dbf.newDocumentBuilder().parse(reader);

            Element root = xmlDoc.getDocumentElement();
            if (!"plist".equals(root.getNodeName())) {
                throw new IOException("XML document is not a plist");
            }
            return parseMap(getFirstChild(root));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // @see "https://stackoverflow.com/questions/2299807/element-firstchild-is-returning-textnode-instead-of-an-object-in-ff"
    private static Node getFirstChild(Node el) {
        Node firstChild = el.getFirstChild();
        while (firstChild != null && firstChild.getNodeType() == 3) { // skip TextNodes
            firstChild = firstChild.getNextSibling();
        }
        return firstChild;
    }

    private static Node getNextSibling(Node el) {
        Node nextSibling = el.getNextSibling();
        while (nextSibling != null && nextSibling.getNodeType() == 3) { // skip TextNodes
            nextSibling = nextSibling.getNextSibling();
        }
        return nextSibling;
    }

    public static void write(Stream stream, Map<String, Object> plist) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();

            Document xmlDoc = dbf.newDocumentBuilder().newDocument();
            DOMImplementation domImpl = dbf.newDocumentBuilder().getDOMImplementation();

            Properties props = new Properties();
            props.setProperty(OutputKeys.INDENT, "yes");
            props.setProperty(OutputKeys.ENCODING, "utf-8");

            DocumentType xmlDocType = domImpl.createDocumentType("plist",
                                                                 "-//Apple//DTD PLIST 1.0//EN",
                                                                 "http://www.apple.com/DTDs/PropertyList-1.0.dtd");
            xmlDoc.appendChild(xmlDocType);
            Element rootElement = xmlDoc.createElement("plist");
            rootElement.setAttribute("Version", "1.0");
            xmlDoc.appendChild(rootElement);
            xmlDoc.getDocumentElement().setAttribute("Version", "1.0");
            rootElement.appendChild(createNode(xmlDoc, plist));

            StreamResult result = new StreamResult(new StreamOutputStream(stream));
            Source source = new DOMSource(xmlDoc);
            transformer.transform(source, result);
        } catch (TransformerException | ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object parseNode(Node xmlNode) {
        String nodeName = xmlNode.getNodeName();
        switch (nodeName) {
        case "dict":
            return parseMap(xmlNode);
        case "array":
            return parseArray(xmlNode);
        case "string":
            return parseString(xmlNode);
        case "data":
            return parseData(xmlNode);
        case "integer":
            return parseInteger(xmlNode);
        case "true":
            return true;
        case "false":
            return false;
        default:
            throw new UnsupportedOperationException();
        }
    }

    private static Node createNode(Document xmlDoc, Object obj) {
        if (obj instanceof Map) {
            return createMap(xmlDoc, (Map) obj);
        }

        if (obj instanceof String) {
            Text text = xmlDoc.createTextNode((String) obj);
            Element node = xmlDoc.createElement("string");
            node.appendChild(text);
            return node;
        }

        throw new UnsupportedOperationException();
    }

    private static Node createMap(Document xmlDoc, Map<String, Object> dict) {
        Element dictNode = xmlDoc.createElement("dict");
        for (Map.Entry<String, Object> entry : dict.entrySet()) {
            Text text = xmlDoc.createTextNode(entry.getKey());
            Element keyNode = xmlDoc.createElement("key");
            keyNode.appendChild(text);
            dictNode.appendChild(keyNode);
            Node valueNode = createNode(xmlDoc, entry.getValue());
            dictNode.appendChild(valueNode);
        }
        return dictNode;
    }

    private static Map<String, Object> parseMap(Node xmlNode) {
        Map<String, Object> result = new HashMap<>();
        Node focusNode = getFirstChild(xmlNode);
        while (focusNode != null) {
            if (!"key".equals(focusNode.getNodeName())) {
                throw new dotnet4j.io.IOException("Invalid plist, expected dictionary key");
            }

            String key = focusNode.getTextContent();
            focusNode = getNextSibling(focusNode);
            result.put(key, parseNode(focusNode));
            focusNode = getNextSibling(focusNode);
        }
        return result;
    }

    private static Object parseArray(Node xmlNode) {
        List<Object> result = new ArrayList<>();
        Node focusNode = getFirstChild(xmlNode);
        while (focusNode != null) {
            result.add(parseNode(focusNode));
            focusNode = getNextSibling(focusNode);
        }
        return result;
    }

    private static Object parseString(Node xmlNode) {
        return xmlNode.getTextContent();
    }

    private static Object parseData(Node xmlNode) {
        String base64 = xmlNode.getTextContent();
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return base64;
        }
    }

    private static Object parseInteger(Node xmlNode) {
        return Integer.parseInt(xmlNode.getTextContent());
    }
}