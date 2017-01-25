/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

/**
 *
 * @author glazen
 */
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class WriteXMLFile_deltas_Client {

    private String byteCount = null;
    private String startTime = null;
    private String endTime = null;
    //DataMeasurement Measurement = null;

    public WriteXMLFile_deltas_Client(String side, Vector<Long> deltaINUplink, Vector<Long> deltasOUTDownlink) {

        try {
            //this.Measurement = _Measurement;
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("SamplesPacketTrains");
            doc.appendChild(rootElement);

            for (int i = 0; i < deltaINUplink.size(); i++) {
                String deltaIN_uplink = String.valueOf(deltaINUplink.get(i));
                String deltaOUT_downlink = String.valueOf(deltasOUTDownlink.get(i));
                rootElement.appendChild(getSample(doc, String.valueOf(i), deltaIN_uplink,deltaOUT_downlink));
            }
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy:HH.mm.SS");
            Date now = new Date();
            String date = DATE_FORMAT.format(now);
            String xmlName = side + "" + date;
            System.err.println("xmlName: " + xmlName);
            StreamResult result = new StreamResult(new File("/Users/glazen/Desktop/Measurements/" + xmlName + ".xml"));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);
            transformer.transform(source, result);

            System.out.println("File saved!");

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }

    private Node getSample(Document doc, String id, String _deltaIN_uplink, String _deltaOUT_downlink) {
        Element sample = doc.createElement("Deltas_Client");
        sample.setAttribute("id", id);
        sample.appendChild(getSampleElements(doc, sample, "deltaIN_uplink", _deltaIN_uplink));
        sample.appendChild(getSampleElements(doc, sample, "deltaOUT_downlink", _deltaOUT_downlink));
        return sample;
    }

    // utility method to create text node
    private Node getSampleElements(Document doc, Element element, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }

}
