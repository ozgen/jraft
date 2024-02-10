package com.ozgen.jraft.node;

import com.ozgen.jraft.model.node.NodeData;

import java.util.concurrent.ConcurrentHashMap;

public interface NodeServerFactory {
    NodeServer createNodeServer(String nodeId, ConcurrentHashMap<String, NodeData> cluster);
}
