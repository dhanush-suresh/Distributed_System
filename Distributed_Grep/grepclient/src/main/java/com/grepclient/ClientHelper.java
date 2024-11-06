package com.grepclient;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.beans.ServerOutput;

import java.util.ArrayList;
import java.util.List;

public class ClientHelper implements Runnable {
    String address;
    int port;
    String pattern_input;
    String options;
    public int matchedCount = 0;
    public List<String> input_List;
    public String log_file_path;
    public ClientHelper(String address, int port, String options, String pattern_input, String log_file_path) {
        this.address = address;
        this.port = port;
        this.options = options;
        this.pattern_input = pattern_input;
        this.log_file_path = log_file_path;
        this.input_List = new ArrayList<>();
        this.input_List.add(options);
        this.input_List.add(pattern_input);
        this.input_List.add(log_file_path);
    }

    public void run() {
        try {
            File directory = new File("./output");
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
            }

            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), 5000);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            oos.writeObject(input_List); // Write inputted Grep command

            ServerOutput respCount = (ServerOutput) ois.readObject(); //Reading count
            matchedCount = Integer.parseInt(respCount.getMatchedCount());
            if (matchedCount > 0) {
                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);

                // First, read the file size sent by the server
                long fileSize = dataInputStream.readLong();

                // Create output stream to write the file to disk
                FileOutputStream fileOutputStream = new FileOutputStream("./output/received_file_"+this.log_file_path);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                // Buffer to store chunks of data
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                // Read the file in chunks
                while (totalBytesRead < fileSize && (bytesRead = inputStream.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                bufferedOutputStream.close();
            }
            oos.close();
            socket.close();
        } catch (Exception e) {
            // System.err.println(e);
            matchedCount = -1;
        }
    }
}
