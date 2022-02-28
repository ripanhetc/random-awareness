package xmlcomparator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CompareXmls {
	static PrintWriter pw = null;
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		
		pw = new PrintWriter(new File("Report.txt"));
		String original = "Sample.xml";
		String comp = "Compare.xml";
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(original));
        
        Document docComp = dBuilder.parse(new File(comp));
        
        doc.getDocumentElement().normalize();
        docComp.getDocumentElement().normalize();
        
        NodeList nodeListOrig = doc.getChildNodes();
        NodeList nodeListComp = docComp.getChildNodes();
        compare(nodeListOrig,nodeListComp);
        pw.close();
	}
	
	public static void compare(NodeList nodeListOrig,NodeList nodeListComp)
	{
		for(int i=0;i<nodeListOrig.getLength();i++)
		{
			Node nodeOrig = nodeListOrig.item(i);
			if(nodeOrig.getNodeType()==Node.ELEMENT_NODE)
			{
				boolean found = false;
				for(int j=0;j<nodeListComp.getLength();j++)
				{
					Node nodeComp = nodeListComp.item(j);
					if(nodeComp.getNodeType()==Node.ELEMENT_NODE && nodeOrig.getNodeName().equals(nodeComp.getNodeName()) && (!nodeComp.getTextContent().equals("visited")))
					{
						found = true;
						compare(nodeOrig.getChildNodes(),nodeComp.getChildNodes());
						//to know that the node is already visited. important for Array cases
						nodeComp.setTextContent("visited");
						break;
					}
				}
				if(!found)
				{
					pw.println(getNodeString(nodeOrig));
					pw.println("-------------------------------");
				}
			}
				
		}
	}
	
	static String getNodeString(Node node) {
	    try {
	        StringWriter writer = new StringWriter();
	        Transformer transformer = TransformerFactory.newInstance().newTransformer();
	        transformer.transform(new DOMSource(node), new StreamResult(writer));
	        String output = writer.toString();
	        //remove <?xml version="1.0" encoding="UTF-8"?>
	        return output.substring(output.indexOf("?>") + 2);
	    } catch (TransformerException e) {
	        e.printStackTrace();
	    }
	    return node.getTextContent();
	}
}
