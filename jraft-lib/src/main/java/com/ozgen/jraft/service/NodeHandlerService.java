package com.ozgen.jraft.service;

import com.ozgen.jraft.model.node.Node;
import com.ozgen.jraft.model.node.NodeResponse;

import java.util.concurrent.CompletableFuture;

public interface NodeHandlerService {

    //todo add implementaion class like message handler
    CompletableFuture<NodeResponse> joinCluster(Node node);
    CompletableFuture<NodeResponse> leaveCluster(String nodeId);
}
