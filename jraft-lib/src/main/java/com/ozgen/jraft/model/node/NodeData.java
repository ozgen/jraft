package com.ozgen.jraft.model.node;

public class NodeData {

    private String ipAddress;

    private int port;

    public NodeData(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }
}
