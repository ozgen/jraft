package com.ozgen.jraft.service.impl;

import com.google.inject.Singleton;
import com.jraft.MessageHandlerServiceGrpc;
import com.ozgen.jraft.model.Message;
import com.ozgen.jraft.model.converter.GrpcToMsgConverter;
import com.ozgen.jraft.model.converter.MsgToGrpcConverter;
import com.ozgen.jraft.service.MessageHandlerService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class MessageHandlerServiceImpl implements MessageHandlerService {

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
    public Message handleVoteRequest(Message message) {
        return null;
    }

    @Override
    public Message handleVoteResponse(Message message) {
        return null;
    }

    @Override
    public Message handleLogRequest(Message message) {
        return null;
    }

    @Override
    public Message handleLogResponse(Message message) {
        return null;
    }
}
