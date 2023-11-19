package com.ozgen.jraft.service.impl;

import com.google.inject.Singleton;
import com.ozgen.jraft.NodeServer;
import com.ozgen.jraft.model.node.Node;
import com.ozgen.jraft.model.node.NodeResponse;
import com.ozgen.jraft.service.NodeHandlerService;

import java.util.concurrent.CompletableFuture;

@Singleton
public class DefaultNodeHandlerService implements NodeHandlerService {

    private final NodeServer nodeServer;

    public DefaultNodeHandlerService(NodeServer nodeServer) {
        this.nodeServer = nodeServer;
    }

    @Override
    public CompletableFuture<NodeResponse> joinCluster(Node node) {
        return this.nodeServer.joinCluster(node.getId(), node.getNodeData());
    }

    @Override
    public CompletableFuture<NodeResponse> leaveCluster(String nodeId) {
        return this.nodeServer.leaveCluster(nodeId);
    }
}
