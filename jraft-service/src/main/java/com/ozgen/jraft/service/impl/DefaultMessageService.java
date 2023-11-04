package com.ozgen.jraft.service.impl;

import com.ozgen.jraft.model.Message;
import com.ozgen.jraft.service.MessageHandlerService;

import java.util.concurrent.CompletableFuture;

public class DefaultMessageService implements MessageHandlerService {

    @Override
    public CompletableFuture<Message> handleVoteRequest(Message message) {
        return null;
    }

    @Override
    public CompletableFuture<Message> handleLogRequest(Message message) {
        return null;
    }
}
