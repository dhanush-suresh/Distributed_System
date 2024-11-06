package com.swim;

import java.io.*;
import java.net.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

public class Node {
    String id;
    boolean isAlive;
    long lastHeartbeat;
    String ipAddress;
    int port;
    DatagramSocket socket;
    int incarnationNumber;
    Node(String id, String ipAddress, int port) {
        this.id = id;
        this.isAlive = true;
        this.lastHeartbeat = System.currentTimeMillis();
        this.ipAddress = ipAddress;
        this.port = port;
        this.incarnationNumber = 0; 
        try {
            this.socket = new DatagramSocket(this.port);
        } catch (Exception e) {
            System.err.println("Failed to create socket: " + e.getMessage());
        }
    }

    public String createCustomXML(String rootElementName, String data) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            org.w3c.dom.Element rootElement = doc.createElement(rootElementName);
            doc.appendChild(rootElement);
            rootElement.appendChild(doc.createTextNode(data));
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
            return writer.toString();
        } catch (ParserConfigurationException | TransformerException e) {
            System.err.println("Error creating XML: " + e.getMessage());
            return null;
        }
    }

    public void sendPing(String target) {
        try {
            String pingMessage = createCustomXML("PING", this.id);
            byte[] buffer = pingMessage.getBytes();
            String[] targetDetails = target.split(";");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(targetDetails[0]),
                    Integer.parseInt(targetDetails[1]));
            socket.send(packet);
            // System.out.println("Ping sent to " + target);
        } catch (IOException e) {
            System.err.println("Error sending ping: " + e.getMessage());
        }
    }

    public void receivePing(String sender, Boolean isNotDropped) {
        if (isNotDropped) {
            // System.out.println("Ping received from " + sender);
            sendAcknowledgment(sender);
        }
    }

    public void sendAcknowledgment(String target) {
        try {
            String ackMessage = createCustomXML("ACK", this.id);
            byte[] buffer = ackMessage.getBytes();
            String[] targetDetails = target.split(";");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(targetDetails[0]),
                    Integer.parseInt(targetDetails[1]));
            socket.send(packet);
            // System.out.println("ACK sent to " + target);
        } catch (IOException e) {
            System.err.println("Error sending acknowledgment: " + e.getMessage());
        }
    }

    public void incrementIncarnation() {
        incarnationNumber++;
    }
}
