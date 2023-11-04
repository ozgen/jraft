package com.ozgen.jraft.service.impl;

import com.google.inject.Singleton;
import com.jraft.MessageHandlerServiceGrpc;
import com.ozgen.jraft.model.Message;
import com.ozgen.jraft.model.converter.GrpcToMsgConverter;
import com.ozgen.jraft.model.converter.MsgToGrpcConverter;
import com.ozgen.jraft.service.MessageHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@Singleton
public class MessageHandlerServiceImpl implements MessageHandlerService {

    private static final Logger logger =  LoggerFactory.getLogger(MessageHandlerService.class);


    private final MessageHandlerServiceGrpc.MessageHandlerServiceImplBase stub;

    private final GrpcToMsgConverter grpcToMsgConverter;
    private final MsgToGrpcConverter msgToGrpcConverter;

    public MessageHandlerServiceImpl(final MessageHandlerServiceGrpc.MessageHandlerServiceImplBase stub,
                                     GrpcToMsgConverter grpcToMsgConverter, MsgToGrpcConverter msgToGrpcConverter) {
        this.stub = stub;
        this.grpcToMsgConverter = grpcToMsgConverter;
        this.msgToGrpcConverter = msgToGrpcConverter;
    }

    //todo after implementing the jraft-service update this class
    // and return null
    @Override
    public CompletableFuture<Message> handleVoteRequest(Message message) {
        return null;
    }

    @Override
    public CompletableFuture<Message> handleLogRequest(Message message) {
        return null;
    }
}
