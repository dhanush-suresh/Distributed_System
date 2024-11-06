package com.swim;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

class SwimProtocol {
    private static final Logger LOGGER = Logger.getLogger(SwimProtocol.class.getName());
    private static FileHandler fileHandler;
    private final List<String> memberList = Collections.synchronizedList(new ArrayList<>());
    private final Node selfNode;
    private static String introducerIp;
    private static int introducerPort;
    private final Map<String, Long> pingMap = new ConcurrentHashMap<>();
    private final Map<String, Long> suspectedNodes = new ConcurrentHashMap<>();
    private static final int pingTimeout = 4000; // Timeout for ACK in milliseconds
    private static final int t_s = 2000; // Time period for suspicion before declaring failure
    private static final int k = 4; // Number of random nodes to ping
    private static Boolean isSuspicionEnabled = false;
    private double messageDropRate = 0.0;
    static {
        try {
            // Create a FileHandler for logging to a file
            fileHandler = new FileHandler("swim_protocol.log", false);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            
            // Remove the default ConsoleHandler
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                if (handler instanceof ConsoleHandler) {
                    rootLogger.removeHandler(handler);
                }
            }
            
            // Add the FileHandler to the logger
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public SwimProtocol(Node selfNode) {
        this.selfNode = selfNode;
        LOGGER.info("SwimProtocol initialized for node: " + selfNode.id);
    }

    public void startProtocol() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        new Thread(this::startServer).start();
        executor.scheduleAtFixedRate(this::pingSelectedNodes, 0, 4, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::checkForFailures, 0, 2, TimeUnit.SECONDS);
        
        LOGGER.info("Protocol started for node: " + selfNode.id);
    }

    public Boolean isNotDropped() {
        Random random = new Random();
        double randomValue = random.nextDouble() * 100;

        if (randomValue < messageDropRate) {
            return false;
        } else {
            return true;
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

    public void joinIntroducer() {
        try {
            System.out.println("Joining Group");
            LOGGER.info("Attempting to join group through introducer: " + introducerIp + ":" + introducerPort);
            String message = selfNode.id;
            String xmlString = createCustomXML("JOIN", message);
            byte[] buffer = xmlString.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(introducerIp),
                    introducerPort);
            selfNode.socket.send(packet);

            // Receive membership list
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            selfNode.socket.receive(receivePacket);
            String membershipMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            processInputMessage(membershipMessage);
            LOGGER.info("Successfully joined group");
        } catch (IOException e) {
            System.err.println("Error joining introducer: " + e.getMessage());
            LOGGER.severe("Error joining introducer: " + e.getMessage());
        }
    }

    public void leaveGroup() {
        System.out.println("Leaving Group");
        broadcastMessage("REMOVE", this.selfNode.id);
        this.selfNode.socket.close();
    }

    public static void enableSuspicion() {
        System.out.println("Enabled Suspicion");
        isSuspicionEnabled = true;
    }

    public static void disableSuspicion() {
        System.out.println("Disabled Suspicion");
        isSuspicionEnabled = false;
    }

    private void processInputMessage(String membershipMessage) {
        Document xmlDocument = convertStringToXMLDocument(membershipMessage);
        String[] nodes;
        if (xmlDocument != null) {
            String messageType = xmlDocument.getDocumentElement().getNodeName();
            nodes = xmlDocument.getDocumentElement().getTextContent().split(",");
            switch (messageType) {
                case "ADD":
                    for (String s : nodes) {
                        System.out.println("Adding node s: " + s);
                        if(!s.equals(""))
                            memberList.add(s);
                    }
                    // Debug
                    String memberListString = "Length: " + memberList.size();
                    for (String member : memberList) {
                        memberListString += " " + member;
                    }
                    System.out.println("Member List after Add: " + memberListString);
                    System.out.println("Received membership list of size: " + memberList.size());
                    LOGGER.info("Received membership list. Size: " + memberList.size());
                    break;
                case "PING":
                    String node = xmlDocument.getDocumentElement().getTextContent();
                    selfNode.receivePing(node, isNotDropped());
                    LOGGER.fine("Received PING from node: " + node);
                    break;
                case "ACK":
                    String sender = xmlDocument.getDocumentElement().getTextContent();
                    // System.out.println("Received Acknowledgement from " + sender);
                    pingMap.remove(sender);
                    LOGGER.fine("Received ACK from node: " + sender);
                    break;
                case "FAIL":
                    String failedNode = xmlDocument.getDocumentElement().getTextContent();
                    System.out.println("Node " + failedNode + " has failed. Removing from membership list.");
                    memberList.remove(failedNode);
                    suspectedNodes.remove(failedNode);
                    LOGGER.warning("Node " + failedNode + " has failed and been removed from membership list");
                    break;
                case "SUSPICION":
                    if (isSuspicionEnabled) {
                        String suspectedNode = xmlDocument.getDocumentElement().getTextContent();
                        System.out.println("Node " + suspectedNode + " is suspected.");
                        suspectedNodes.put(suspectedNode, System.currentTimeMillis());
                        LOGGER.warning("Node " + suspectedNode + " is suspected");
                    }
                    break;
                case "REMOVE":
                    memberList.removeAll(Arrays.asList(nodes));
                    System.out.println("Removed nodes");
                    break;
                case "UPDATE_DROP_RATE":
                    messageDropRate = Double.parseDouble(xmlDocument.getDocumentElement().getTextContent());
                    break;
                case "ENABLE":
                    enableSuspicion();
                    break;
                case "DISABLE":
                    disableSuspicion();
                    break;
                case "INCARNATION":
                    if (isSuspicionEnabled) {
                        String[] data = xmlDocument.getDocumentElement().getTextContent().split(",");
                        String nodeId = data[0];
                        int newIncarnation = Integer.parseInt(data[1]);
                        updateNodeIncarnation(nodeId, newIncarnation);
                    }
                    break;
                default:
                    System.out.println("Received unknown message type: " + messageType);
                    break;
            }
        }
    }

    private void updateNodeIncarnation(String nodeId, int newIncarnation) {
        for (String member : memberList) {
            if (member.startsWith(nodeId)) {
                String[] details = member.split(";");
                int currentIncarnation = Integer.parseInt(details[2]);
                if (newIncarnation > currentIncarnation) {
                    // Update the member list with new incarnation number
                    memberList.remove(member);
                    memberList.add(nodeId + ";" + details[0] + ";" + newIncarnation);
                    System.out.println("Updated incarnation number for " + nodeId + " to " + newIncarnation);
                }
                break;
            }
        }
    }

    public void startServer() {
    
        System.out.println("Starting the server");
        LOGGER.info("Starting the server for node: " + selfNode.id);
        try {
            byte[] receiveBuffer = new byte[1024];
            this.selfNode.socket.setSoTimeout(1000); // so we aren't stuck waiting for a UDP packet and can take in
                                                     // input
            Scanner scanner = new Scanner(System.in);
            String input;
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    this.selfNode.socket.receive(receivePacket);
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    processInputMessage(message);
                } catch (Exception e) {
                    // System.err.println("No UDP packet received");
                }

                if (System.in.available() > 0) {
                    input = scanner.nextLine();
                    System.out.println("Input: " + input);
                    switch (input) {
                        case "leave":
                            leaveGroup();
                            break;
                        case "list_mem":
                            String memberListString = "Length: " + memberList.size();
                            for (String member : memberList) {
                                memberListString += " " + member;
                            }
                            System.out.println("Member List: " + memberListString);
                            break;
                        case "list_self":
                            System.out.println("NodeID: " + this.selfNode.id);
                            break;
                        case "status_sus":
                            System.out.println("Suspicion Status: " + isSuspicionEnabled);
                            break;
                        case "display_sus":
                            String susList = "";
                            for (String nodeId : suspectedNodes.keySet()) {
                                susList += " " + nodeId;
                            }
                            System.out.println("Suspected Node List: " + susList);
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            LOGGER.severe("Server error: " + e.getMessage());
        }
    }

    public void pingSelectedNodes() {
        if (memberList.isEmpty())
            return;
        // Select k random nodes to ping
        List<String> randomNodes = selectKNeighbours();
        for (String randomNode : randomNodes) {
            if (!randomNode.equals(selfNode.id) && !pingMap.containsKey(randomNode)) { // Exclude nodes already being
                                                                                       // pinged
                pingMap.put(randomNode, System.currentTimeMillis());
                if (isNotDropped()) {
                    selfNode.sendPing(randomNode);
                    LOGGER.fine("Sent PING to node: " + randomNode);
                }
            }
        }
    }

    private List<String> selectKNeighbours() {
        List<String> randomNodes = new ArrayList<>();
        // If memberList has no other members
        if (memberList.size() <= 1) {
            return randomNodes;
        }
    
        // Get the index of the selfNode in the memberList
        int selfIndex = memberList.indexOf(selfNode.id);
        if (selfIndex == -1) {
            return randomNodes;
        }
    
        // Number of predecessors and successors
        int halfK = Math.min(k / 2, (memberList.size() - 1) / 2);  // -1 to exclude selfNode
    
        // Select predecessors
        for (int i = 1; i <= halfK; i++) {
            int predecessorIndex = (selfIndex - i + memberList.size()) % memberList.size();
            if (!memberList.get(predecessorIndex).equals(selfNode.id)) {
                randomNodes.add(memberList.get(predecessorIndex));
            }
        }
        // Select successors
        for (int i = 1; i <= halfK; i++) {
            int successorIndex = (selfIndex + i) % memberList.size();
            if (!memberList.get(successorIndex).equals(selfNode.id)) {
                randomNodes.add(memberList.get(successorIndex));
            }
        }
        // If there are fewer than k nodes, ensure we get all available nodes (excluding selfNode)
        if (randomNodes.size() < k) {
            for (String node : memberList) {
                if (!node.equals(selfNode.id) && !randomNodes.contains(node)) {
                    randomNodes.add(node);
                    if (randomNodes.size() == k) break; // Stop once we reach k nodes
                }
            }
        }
        LOGGER.fine("Nodes Selected to PING " + randomNodes);
        return randomNodes;
    }

    public void checkForFailures() {
        long currentTime = System.currentTimeMillis();

        // Check for nodes that didn't respond
        pingMap.forEach((node, timestamp) -> {
            if (!node.equals("")) {
                if (currentTime - timestamp > pingTimeout) {
                    if (isSuspicionEnabled) {
                        if (!suspectedNodes.containsKey(node)) {
                            System.out.println("Node " + node + " is suspected (no ACK received).");
                            suspectedNodes.put(node, currentTime);
                            broadcastMessage("SUSPICION", node);
                            LOGGER.warning("Node " + node + " is suspected (no ACK received)");
                        }
                        pingMap.remove(node);
                    } else {
                        System.out.println("Node " + node + " has failed (no ACK received).");
                        pingMap.remove(node);
                        memberList.remove(node);
                        broadcastMessage("FAIL", node);
                        LOGGER.warning("Node " + node + " has failed (no ACK received)");
                    }
                }
            }
        });

        List<String> nodesToRemove = new ArrayList<>();

        if (isSuspicionEnabled) {
            suspectedNodes.forEach((node, timestamp) -> {
                if (currentTime - timestamp > t_s) {
                    System.out.println("Node " + node + " has failed (confirmed after suspicion period).");
                    nodesToRemove.add(node);
                    memberList.remove(node);
                    broadcastMessage("FAIL", node);
                }
            });
        }

        nodesToRemove.forEach(suspectedNodes::remove);
    }

    public void broadcastMessage(String action, String node) {
        try {
            String broadcastMessage = selfNode.createCustomXML(action, node);
            if ("INCARNATION".equals(action) && isSuspicionEnabled) {
                int incarnation = selfNode.incarnationNumber;
                // Get the incarnation number
                broadcastMessage = selfNode.createCustomXML("INCARNATION", node + "," + incarnation);
            }
            byte[] buffer = broadcastMessage.getBytes();

            // Broadcast to all nodes
            for (String member : memberList) {
                if (!member.equals(selfNode.id)) {
                    String[] memberDetails = member.split(";");
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                            InetAddress.getByName(memberDetails[0]),
                            Integer.parseInt(memberDetails[1]));
                    if (isNotDropped()) {
                        this.selfNode.socket.send(packet);
                    }
                }
            }

            // Notify the introducer
            DatagramPacket introducerPacket = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(introducerIp), introducerPort);
            this.selfNode.socket.send(introducerPacket);
            LOGGER.info("Broadcasted " + action + " message for node: " + node);
        } catch (IOException e) {
            System.err.println("Error broadcasting failure: " + e.getMessage());
            LOGGER.severe("Error broadcasting message: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java SwimProtocol <introducer-ip> <introducer-port>");
            return;
        }

        introducerIp = args[0];
        introducerPort = Integer.parseInt(args[1]);

        Random random = new Random();
        int port = 6002;
        long time_stamp = System.currentTimeMillis();
        InetAddress localHost;
        try {
            localHost = InetAddress.getLocalHost();
            String ipAddress = localHost.getHostAddress();
            String nodeId = ipAddress.concat(";").concat(Integer.toString(port)).concat(";")
                    .concat(Long.toString(time_stamp));
            Node selfNode = new Node(nodeId, ipAddress, port);
            SwimProtocol swimProtocol = new SwimProtocol(selfNode);

            swimProtocol.joinIntroducer();
            swimProtocol.startProtocol();
            LOGGER.info("SwimProtocol main method completed initialization");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }
}
