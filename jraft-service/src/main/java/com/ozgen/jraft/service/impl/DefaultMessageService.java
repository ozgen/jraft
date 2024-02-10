package com.ozgen.jraft.service.impl;

import com.google.inject.Singleton;
import com.ozgen.jraft.model.message.Message;
import com.ozgen.jraft.node.NodeServer;
import com.ozgen.jraft.service.MessageHandlerService;

import java.util.concurrent.CompletableFuture;

@Singleton
public class DefaultMessageService implements MessageHandlerService {

    private NodeServer nodeServer;

    @Override
    public void setNodeServer(NodeServer nodeServer) {
        this.nodeServer = nodeServer;
    }

    @Override
    public CompletableFuture<Message> handleVoteRequest(Message message) {
        return this.nodeServer.handleVoteRequest(message);
    }

    @Override
    public CompletableFuture<Message> handleLogRequest(Message message) {
        return this.nodeServer.handleLogRequest(message);
    }
}
