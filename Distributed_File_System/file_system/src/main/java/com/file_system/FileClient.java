package com.file_system;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class FileClient implements Runnable {
    private int serverPort;
    private String serverAddress;
    private String sourceFileName;
    private String destinationFileName;
    private String replication_status;
    public FileClient(String address, String port, String sourceFileName, String destinationFileName, String replication_status) {
        this.serverAddress = address;
        this.serverPort = Integer.parseInt(port);
        this.sourceFileName = sourceFileName;
        this.destinationFileName = destinationFileName;
        this.replication_status = replication_status;
    }
    @Override
    public void run() {

        //TODO:
        // Send: Filename, filesize, File
        // Read file from local_file
        // Send filename
        // send filesize using writeLong
        // Send file
        try {
            System.out.println("Starting Client");
            System.out.println("Server Address: " + this.serverAddress + " Server Port: " + this.serverPort);
            
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(this.serverAddress, this.serverPort), 5000);
            OutputStream outputStream = socket.getOutputStream(); // To send files
            File sourceFile = new File(this.sourceFileName);
            FileInputStream fileInputStream = new FileInputStream(sourceFile);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

            dataOutputStream.writeUTF(this.replication_status);
            // Send file name:
            System.out.println("DEBUG:- Sending file name");
            dataOutputStream.writeUTF(this.destinationFileName);
            System.out.println("DEBUG:- Sent file name");

            // Send file size to the client first
            System.out.println("DEBUG:- Sending file size");
            dataOutputStream.writeLong(sourceFile.length());
            System.out.println("DEBUG:- Sent file size");

            // Sending files in chunks
            System.out.println("DEBUG:- Sending file");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
            System.out.println("DEBUG:- Sent file");

            System.out.println("Closing connection");
            outputStream.close();
            dataOutputStream.close();
            bufferedInputStream.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error while sending file: " + e);
        }
    }
    
}
