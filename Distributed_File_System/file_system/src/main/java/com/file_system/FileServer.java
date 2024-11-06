package com.file_system;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class FileServer implements Runnable {
    private int port;
    private int serverPort;

    public FileServer(int port, int serverPort) {
        this.port = port;
        this.serverPort = serverPort;
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

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("FileServer started on port " + port);

            while (true) {
                System.out.println("FileServer waiting for a client...");
                try (Socket clientSocket = server.accept();
                        DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream())) {

                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // Read file metadata
                    String replication_status = dataInputStream.readUTF();
                    String fileName = dataInputStream.readUTF();
                    long fileSize = dataInputStream.readLong();

                    // Create output stream to write the file to disk
                    File outputFile = new File("./data_storage/HyDFS/" + fileName);
                    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

                        byte[] buffer = new byte[8192];
                        long totalBytesRead = 0;
                        int bytesRead;

                        // Read the file in chunks
                        while (totalBytesRead < fileSize && (bytesRead = dataInputStream.read(buffer)) != -1) {
                            bufferedOutputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                        
                            String pingMessage = createCustomXML("FILE_RECEIVED", fileName+";"+replication_status+";"+fileSize);
                            byte[] message_buffer = pingMessage.getBytes();
                            DatagramPacket packet = new DatagramPacket(message_buffer, message_buffer.length,InetAddress.getByName("127.0.0.1"),this.serverPort);
                            DatagramSocket socket = new DatagramSocket(7002);
                            socket.send(packet);
                            socket.close();

                        System.out.println("File " + fileName + " received successfully.");
                    } catch (IOException e) {
                        System.err.println("Error writing file " + fileName + ": " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("Error handling client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("FileServer error: " + e.getMessage());
        }
    }
}
