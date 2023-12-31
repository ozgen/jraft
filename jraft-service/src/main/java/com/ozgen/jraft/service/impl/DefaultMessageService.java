package com.ozgen.jraft.service.impl;

import com.google.inject.Singleton;
import com.ozgen.jraft.NodeServer;
import com.ozgen.jraft.model.message.Message;
import com.ozgen.jraft.service.MessageHandlerService;

import java.util.concurrent.CompletableFuture;

@Singleton
public class DefaultMessageService implements MessageHandlerService {

    private final NodeServer nodeServer;

    public DefaultMessageService(NodeServer nodeServer) {
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
