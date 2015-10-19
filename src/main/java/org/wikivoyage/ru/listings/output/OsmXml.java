package org.wikivoyage.ru.listings.output;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wikivoyage.ru.listings.entity.WikivoyagePOI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class OsmXml implements OutputFormat {
    private static Integer nodeId = 0;
    private boolean userDefined;

    public OsmXml(boolean userDefined) {
        this.userDefined = userDefined;
    }

    @Override
    public void write(WikivoyagePOI[] pois, String outputFilename) throws WriteOutputException {
        try {
            writePOIsToXML(pois, outputFilename, userDefined);
        } catch (ParserConfigurationException | TransformerException e) {
            throw new WriteOutputException();
        }
    }


    public static void writePOIsToXML(WikivoyagePOI[] pois, String outputFilename, boolean userDefined) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element rootElement = doc.createElement("osm");
        rootElement.setAttribute("version", "0.5");
        rootElement.setAttribute("generator", "wikivoyage-pois-converter");
        doc.appendChild(rootElement);

        for (WikivoyagePOI poi: pois) {
            if (poi.getLatitude().equals("") || poi.getLongitude().equals("")) {
                // POIs with no positional data are useless for OsmAnd - do not add them
                // to reduce size of output file
                continue;
            }

            Element node = createPoiNode(doc, poi);
            rootElement.appendChild(node);
            if (!userDefined) {
                addKVNode(doc, node, "wikivoyage", poi.getType());
            } else {
                addKVNode(doc, node, "user_defined", "user_defined");
            }
            addKVNode(doc, node, "name", poi.getTitle());
            addKVNode(doc, node, "description", poi.getDescription());
        }

        writeXmlToFile(doc, outputFilename);
    }

    private static void writeXmlToFile(Document doc, String outputFilename) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        configureIndentation(transformer);
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outputFilename));

        transformer.transform(source, result);
    }

    private static void configureIndentation(Transformer transformer) {
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    }

    private static Element createPoiNode(Document doc, WikivoyagePOI poi) {
        Element node = doc.createElement("node");
        node.setAttribute("id", nodeId.toString());
        node.setAttribute("visible", "true");
        node.setAttribute("lat", poi.getLatitude());
        node.setAttribute("lon", poi.getLongitude());
        nodeId++;
        return node;
    }

    private static void addKVNode(Document doc, Element rootNode, String name, String value)
    {
        Element newNode = doc.createElement("tag");
        newNode.setAttribute("k", name);
        newNode.setAttribute("v", value);
        rootNode.appendChild(newNode);
    }

    public String getDefaultExtension()
    {
        if (userDefined) {
            return ".user-defined.xml";
        } else {
            return ".xml";
        }
    }
}
