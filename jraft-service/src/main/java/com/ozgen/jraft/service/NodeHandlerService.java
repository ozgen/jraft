package com.ozgen.jraft.service;

import com.ozgen.jraft.model.node.Node;
import com.ozgen.jraft.model.node.NodeResponse;
import com.ozgen.jraft.node.NodeServer;

import java.util.concurrent.CompletableFuture;

public interface NodeHandlerService {

    CompletableFuture<NodeResponse> joinCluster(Node node);
    CompletableFuture<NodeResponse> leaveCluster(String nodeId);
    void setNodeServer(NodeServer nodeServer);
}
