package com.grepclient;

import java.net.Socket;
import java.util.Scanner;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;

public class App {
    Socket socket = null;
    Scanner scanner = new Scanner(System.in);
    String port = "6001";
    static String networkConfigProps = "/machineConnection.properties";

    public void clearOutputDir(File dir) {
        for(File file: dir.listFiles()) {
            if (!file.isDirectory()) 
            file.delete();
        }
    }

    public App() {
        try {
            File directory = new File("./output");
            if (directory.exists()) {
                clearOutputDir(directory);
            }
            String input = "Start";
            System.out.println("Enter your grep command");
            input = scanner.nextLine();
            scanner.close();
            long startTime = System.currentTimeMillis();
            String command = extractCommand(input);
            String pattern = extractPattern(input);
            String options = '-' + extractOptions(input.replace(pattern, ""));
            if (command != null && pattern != null) {
                if (command.equals("grep")) {
                    InputStream vm_list = getClass().getResourceAsStream(networkConfigProps);
                    Properties machineProperties = new Properties();
                    machineProperties.load(vm_list);
                    String[] hostNames = machineProperties.getProperty("host_name").split(",");
                    String[] logFiles = machineProperties.getProperty("log_file").split(",");
                    int N = hostNames.length;
                    ClientHelper clientObjects[] = new ClientHelper[N];
                    Thread[] clientThread = new Thread[N];
                    // Creating one thread per VM to connect to
                    for (int i = 0; i < N; i++) {
                        clientObjects[i] = new ClientHelper(hostNames[i], Integer.parseInt(port), options, pattern, logFiles[i]);
                        clientThread[i] = new Thread(clientObjects[i]);
                        clientThread[i].start();
                    }
                    int totalCount = 0;
                    for (int i = 0; i < N; i++) {
                        clientThread[i].join();
                        if (clientObjects[i].matchedCount >= 0) {
                            totalCount += clientObjects[i].matchedCount;
                            System.out.println(
                                    "Count for vm" + (i + 1) + ".log processed at " + clientObjects[i].address + ": "
                                            + clientObjects[i].matchedCount);
                        } else {
                            System.out.println(clientObjects[i].address + " is currently offline :( WOMP WOMP");
                        }
                    }
                    long endTime = System.currentTimeMillis();
                    System.out.println("Total Count of Matching line items: " + totalCount);
                    System.out.println("Total Execution time (Milliseconds): " + (endTime - startTime));

                } else {
                    System.out.println("Only grep command supported! (Angry Face). Use grep -options phrase_to_search");
                }
            } else {
                System.out.println("Oops! Invalid Input");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extractCommand(String command) {
        // Split by space and get the first part
        return command.split("\\s+")[0];
    }

    private static String extractOptions(String command) {
        StringBuilder options = new StringBuilder();
        String[] parts = command.split("\\s+");
        for (String part : parts) {
            if (part.startsWith("-")) {
                options.append(part.replace("-", ""));
            }
        }
        return options.toString();
    }

    private static String extractPattern(String command) {
        try {
            int pos = command.indexOf("\"");
            int lastIndex = command.lastIndexOf("\"");
            return command.substring(pos+1, lastIndex);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        App clientApp = new App(); // constructor to put ip address and port
    }
}
