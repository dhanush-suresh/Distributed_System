package com.introducer;

import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

class Introducer {
    private List<String> nodes = new ArrayList<String>(); // IP;Port;timestamp
    private int introducerPort;

    public Introducer(int port) {
        this.introducerPort = port;
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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Document convertStringToXMLDocument(String xmlString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
            return builder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void  sendMembershipListToAll(String nodeId, String action, DatagramSocket socket) {
        System.out.println("Sending update to all other nodes");
        try {
            for (String node : nodes) {
                String[] nodeDetails = node.split(";");
                String xmlString = createCustomXML(action,nodeId);
                System.out.println("String to be sent to all: " + xmlString);
                byte[] buffer = xmlString.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(nodeDetails[0]), Integer.parseInt(nodeDetails[1]));
                socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMembershipListToNewNode(String newNodeIP, String newNodePort, DatagramSocket socket) {
        try {
            StringBuilder membershipList = new StringBuilder();
            for (String node : nodes) {
                membershipList.append(node).append(","); // Append each node followed by a comma
            }
            if (membershipList.length() > 0) {
                membershipList.setLength(membershipList.length() - 1); // Remove the last comma
            }
            String xmlString = createCustomXML("ADD", membershipList.toString());
            byte[] buffer = xmlString.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(newNodeIP), Integer.parseInt(newNodePort));
            socket.send(packet);
            System.out.println("Sent membership list in XML format");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // New method to notify other nodes of a failure
    private void notifyFailureToAll(String failedNodeId, DatagramSocket socket) {
        System.out.println("Notifying all nodes of the failure of node " + failedNodeId);
        try {
            for (String node : nodes) {
                String[] nodeDetails = node.split(";");
                String xmlString = createCustomXML("FAIL", failedNodeId);
                byte[] buffer = xmlString.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(nodeDetails[0]), Integer.parseInt(nodeDetails[1]));
                socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void nodeLeaveGroup(String nodeId) {
        nodes.remove(nodeId);
    }

    private void enableDisableSuspicion(Boolean status, DatagramSocket socket) {
        if(status) {
            sendMembershipListToAll(status.toString(), "ENABLE", socket);
        } else {
            sendMembershipListToAll(status.toString(), "DISABLE", socket);
        }
    }

    public void updateMessageDropRate(String dropRate, DatagramSocket socket) {
        sendMembershipListToAll(dropRate, "UPDATE_DROP_RATE", socket);
    }

    public void startIntroducer() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(introducerPort);
            serverSocket.setSoTimeout(1000); // so we aren't stuck waiting for a UDP packet and can take in input
            Scanner scanner = new Scanner(System.in);
            String input;
            byte[] receiveBuffer = new byte[1024];
            System.out.println("Introducer started on port " + introducerPort);

            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    serverSocket.receive(receivePacket);
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    // Parse the XML message
                    Document xmlDocument = convertStringToXMLDocument(message);
                    if (xmlDocument != null) {
                        String messageType = xmlDocument.getDocumentElement().getNodeName(); // Get the root element name
                        String nodeId = xmlDocument.getDocumentElement().getTextContent();
                        switch (messageType) {
                            case "JOIN": // A new node wants to join
                                String[] nodeDetails = nodeId.split(";"); // Split by ';'
                                String nodeIP = nodeDetails[0];
                                String nodePort = nodeDetails[1];
                                sendMembershipListToAll(nodeId, "ADD", serverSocket);
                                nodes.add(nodeId);
                                sendMembershipListToNewNode(nodeIP, nodePort, serverSocket);
                                System.out.println(nodeId + " has successfully joined.");
                                break;
                            case "REMOVE":
                                nodeLeaveGroup(nodeId);
                                // sendMembershipListToAll(nodeId, "REMOVE", serverSocket);
                                System.out.println(nodeId + " has left the group");
                                break;
                            case "FAIL": // A node has failed
                                String[] nodeDetails1 = nodeId.split(";"); // Split by ';'
                                InetAddress senderAddress = receivePacket.getAddress();
                                String failedNodeId = xmlDocument.getDocumentElement().getTextContent();
                                System.out.println("Node " + failedNodeId + " has failed. Removing from membership list.");
                                System.out.println("Got failure intimation from: " + senderAddress.toString());
                                nodes.remove(failedNodeId);  // Remove the failed node from the introducer's membership list
                                // notifyFailureToAll(failedNodeId, serverSocket);  // Notify all other nodes of the failure
                                break;
                            default:
                                // System.out.println("Received unknown message type: " + messageType);
                                break;
                        }
                    } else {
                        System.out.println("Invalid XML format received.");
                    }
                } catch (Exception e) {
                    // System.out.println("No data to socket");
                }

                if(System.in.available() > 0) {
                    input = scanner.nextLine();
                    System.out.println("Input: " + input);
                    String[] parsedInput = input.split(":");
                    switch(input) {
                        case "enable_sus": //Enable Sus
                            enableDisableSuspicion(true, serverSocket);
                            break;
                        case "disable_sus": // Disable Sus
                            enableDisableSuspicion(false, serverSocket);
                            break;
                        case "update_messageDrop": // input should be update_messageDrop:1.1
                            System.out.println(parsedInput);
                            updateMessageDropRate(parsedInput[1], serverSocket);
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Introducer <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        Introducer introducer = new Introducer(port);
        introducer.startIntroducer();
    }
}
