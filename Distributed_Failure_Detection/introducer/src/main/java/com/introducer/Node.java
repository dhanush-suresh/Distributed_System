package com.introducer;
import java.io.*;
import java.net.*;

public class Node {
    String id;
    boolean isAlive;
    long lastHeartbeat;
    String ipAddress;
    int port;

    Node(String id, String ipAddress, int port) {
        this.id = id;
        this.isAlive = true;
        this.lastHeartbeat = System.currentTimeMillis();
        this.ipAddress = ipAddress;
        this.port = port;
    }
}
