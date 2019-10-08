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

package DiscUtils.Core;

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

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import moe.yo3explorer.dotnetio4j.Stream;
import moe.yo3explorer.dotnetio4j.StreamInputStream;
import moe.yo3explorer.dotnetio4j.StreamOutputStream;


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

            return parseMap(root.getFirstChild());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
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
        String __dummyScrutVar0 = xmlNode.getNodeName();
        if (__dummyScrutVar0.equals("dict")) {
            return parseMap(xmlNode);
        } else if (__dummyScrutVar0.equals("array")) {
            return parseArray(xmlNode);
        } else if (__dummyScrutVar0.equals("string")) {
            return parseString(xmlNode);
        } else if (__dummyScrutVar0.equals("data")) {
            return parseData(xmlNode);
        } else if (__dummyScrutVar0.equals("integer")) {
            return parseInteger(xmlNode);
        } else if (__dummyScrutVar0.equals("true")) {
            return true;
        } else if (__dummyScrutVar0.equals("false")) {
            return false;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static Node createNode(Document xmlDoc, Object obj) {
        if (obj instanceof Map) {
            return createMap(xmlDoc, Map.class.cast(obj));
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
        Node focusNode = xmlNode.getFirstChild();
        while (focusNode != null) {
            if (!"key".equals(focusNode.getNodeName())) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Invalid plist, expected dictionary key");
            }

            String key = focusNode.getTextContent();
            focusNode = focusNode.getNextSibling();
            result.put(key, parseNode(focusNode));
            focusNode = focusNode.getNextSibling();
        }
        return result;
    }

    private static Object parseArray(Node xmlNode) {
        List<Object> result = new ArrayList<>();
        Node focusNode = xmlNode.getFirstChild();
        while (focusNode != null) {
            result.add(parseNode(focusNode));
            focusNode = focusNode.getNextSibling();
        }
        return result;
    }

    private static Object parseString(Node xmlNode) {
        return xmlNode.getTextContent();
    }

    private static Object parseData(Node xmlNode) {
        String base64 = xmlNode.getTextContent();
        return Base64.getDecoder().decode(base64);
    }

    private static Object parseInteger(Node xmlNode) {
        return Integer.parseInt(xmlNode.getTextContent());
    }

}