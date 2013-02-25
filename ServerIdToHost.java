
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class ServerIdToHost {
    static String INFRADB_PATH="http://infradb:3000/devices.xml";
    private static final Logger _log = Logger.getLogger(ServerIdToHost.class);
    static Map<String,String> listOfServers;

    public static Map<String, String> getListOfServers() throws Exception
    {
        if(listOfServers==null)
        {
            InputStream in = null;
            listOfServers=new HashMap<String,String>();
            try {
                //connect to infradb
                URL url = new URL(INFRADB_PATH);
                URLConnection conn = url.openConnection();
                in = conn.getInputStream();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(in);
                doc.getDocumentElement().normalize();
                NodeList nodeLst = doc.getElementsByTagName("device");
                if(_log.isDebugEnabled())
                    _log.debug("Reading the information of all devices from infradb");

                for (int s = 0; s < nodeLst.getLength(); s++) {
                    Node fstNode = nodeLst.item(s);
                    if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element Elmnt = (Element) fstNode;
                        NodeList NmElmntLst = Elmnt.getElementsByTagName("name");
                        Element NmElmnt = (Element) NmElmntLst.item(0);
                        NodeList column2 = NmElmnt.getChildNodes();
                        NodeList idElmntLst = Elmnt.getElementsByTagName("host-id");
                        Element idElmnt = (Element) idElmntLst.item(0);
                        NodeList column1 = idElmnt.getChildNodes();
                        if(_log.isDebugEnabled())
                            _log.debug("Name : "  + column2.item(0).getNodeValue()+"  host-id : " + column1.item(0).getNodeValue());
                        //write the host id(column 1) and name(column 2)
                        listOfServers.put(column1.item(0).getNodeValue(), column2.item(0).getNodeValue());
                    }
                }
                if(_log.isDebugEnabled())
                    _log.debug("Finished Reading.");
            } catch (Exception e) {
                _log.error("Error occured while fetching device information from infradb.", e);
                throw e;
            }
        }
        return listOfServers;
    }


    public static void writeMapToFile(Map<String, String> map, String fileName, String keyValueSeparator, String pairSeparator) throws Exception
    {
        PrintWriter out = null;
        File outFile = null;
        try {
            outFile = new File(fileName);
            out = new PrintWriter(outFile);
            Set<Map.Entry<String,String> > setOfItems=map.entrySet();
            for(Map.Entry<String,String> item : setOfItems)
            {
                out.print(item.getKey() + keyValueSeparator + item.getValue() + pairSeparator);
            }
            out.close();
        }
        catch (Exception e)
        {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (outFile != null)
                outFile.delete();

            _log.error("Failed to output device information to file.", e);
            throw e;
        }
    }


    /*
     * It connects to infradb, gets the mapping of host id and name and writes it to argv[0] (Likely server_id_to_host.txt)
     */
    public static void main(String argv[]) throws Exception {

        Map<String,String> listOfServers =getListOfServers();

        if(_log.isDebugEnabled())
            _log.debug("writing all devices to " + argv[0]);

        writeMapToFile(listOfServers, argv[0], "\t", "\n");
        if(_log.isDebugEnabled())
            _log.debug("Finished writing.");
    }
}

