package com.ozgen.jraft.model.node;

public class Node {

    private final String id;
    private final NodeData nodeData;

    public Node(String id, NodeData nodeData) {
        this.id = id;
        this.nodeData = nodeData;
    }

    public String getId() {
        return id;
    }

    public NodeData getNodeData() {
        return nodeData;
    }
}
