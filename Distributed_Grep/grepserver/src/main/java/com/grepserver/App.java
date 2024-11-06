package com.grepserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import com.beans.ServerOutput;
import java.util.Scanner;
import java.util.List;

public final class App {
    private Socket socket = null;
    private ServerSocket server = null;
    private static String logFilePath = "./log_files/";
    private static String logFileDir = "/home/dsuresh3/MP1/";

    public App(int port) {
        // starts server and waits for a connection
        try {
            server = new ServerSocket(port);
            while (true) {

                System.out.println("Server started");
                System.out.println("Waiting for a client ...");
                socket = server.accept();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Client accepted");

                // takes input from the client socket
                @SuppressWarnings("unchecked")
                List<String> input = (List<String>) ois.readObject();

                String options = input.get(0);
                String pattern = input.get(1);
                String logFile = input.get(2);
                String option1;
                String option2;
                if(options.equals("-")) {  // If no options are provided
                    option1 = "-c";    
                    option2 = "";
                }
                else {
                    option1 = options.concat("c");
                    option2 = options;
                }
                ProcessBuilder processBuilder = new ProcessBuilder("grep", option1, pattern, logFilePath.concat(logFile));
                processBuilder.directory(new File(logFileDir)); // Set the working directory
                Process processCount = processBuilder.start(); // Executing the grep query
                BufferedReader brCount = new BufferedReader(new InputStreamReader(processCount.getInputStream()));

                String totalLineCount = "";
                if ((totalLineCount = brCount.readLine()) != null) {
                    System.out.println("Count: " + totalLineCount);
                }
                ServerOutput result = new ServerOutput();
                result.setMatchedCount(totalLineCount);
                oos.writeObject(result); //Writing line count

                if (Integer.parseInt(totalLineCount) > 0) {
                    File outputFile = new File("output_"+logFile+".txt");
                    ProcessBuilder processBuilder2;
                    if (!option2.equals("")) {
                        processBuilder2 = new ProcessBuilder("grep", option2, pattern, logFilePath.concat(logFile));
                    } else {
                        processBuilder2 = new ProcessBuilder("grep", pattern, logFilePath.concat(logFile));
                    }
                    processBuilder2.directory(new File(logFileDir)); // Set the working directory
                    
                    processBuilder2.redirectOutput(outputFile);
                    Process processLine = processBuilder2.start();
                    int exitCode = processLine.waitFor(); // Waiting for grep query to finish executing before writing to file
                    if (exitCode != 0) {
                        System.out.println("File generation failed with exit code: " + exitCode);
                        return;
                    }

                    FileInputStream fileInputStream = new FileInputStream(outputFile);
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

                    // Get socket output stream to send data
                    OutputStream outputStream = socket.getOutputStream();

                    // Send file size to the client first
                    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                    dataOutputStream.writeLong(outputFile.length());
                    // Create a buffer and send the file in chunks
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                    }
                    outputFile.delete();
                    bufferedInputStream.close();
                    System.out.println("File sent successfully!");
                }
                System.out.println("Closing connection");
                ois.close();
                oos.close();
                socket.close();
            }
        } catch (IOException | ClassNotFoundException | InterruptedException i) {
            System.out.println(i);
            i.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        App serverApp = new App(6001);
        sc.close();
    }
}
