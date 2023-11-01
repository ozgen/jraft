package com.ozgen.jraft.service.impl;

import com.ozgen.jraft.model.Message;
import com.ozgen.jraft.service.MessageHandlerService;

public class DefaultMessageService implements MessageHandlerService {

    @Override
    public Message handleVoteRequest(Message message) {
        return null;
    }

    @Override
    public Message handleLogRequest(Message message) {
        return null;
    }
}
