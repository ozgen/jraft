package com.ozgen.jraft;

import com.google.inject.Singleton;
import com.jraft.Message;
import com.jraft.MessageHandlerServiceGrpc;
import com.ozgen.jraft.model.converter.GrpcToMsgConverter;
import com.ozgen.jraft.model.converter.MsgToGrpcConverter;
import com.ozgen.jraft.service.MessageHandlerService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.CompletionException;

@Singleton
@Slf4j
public class GrpcMessageHandlerServiceImpl extends MessageHandlerServiceGrpc.MessageHandlerServiceImplBase {

    private final GrpcToMsgConverter grpcToMsgConverter;
    private final MsgToGrpcConverter msgToGrpcConverter;
    private final MessageHandlerService messageHandlerService;

    public GrpcMessageHandlerServiceImpl(GrpcToMsgConverter grpcToMsgConverter,
                                         MsgToGrpcConverter msgToGrpcConverter,
                                         MessageHandlerService messageHandlerService) {

        this.grpcToMsgConverter = grpcToMsgConverter;
        this.msgToGrpcConverter = msgToGrpcConverter;
        this.messageHandlerService = messageHandlerService;
    }

    @Override
    public void handleVoteRequest(Message.MessageWrapper request, StreamObserver<Message.MessageWrapper> responseObserver) {
        try {
            com.ozgen.jraft.model.Message message = this.grpcToMsgConverter.convert(request);
            com.ozgen.jraft.model.Message result = this.messageHandlerService.handleVoteRequest(message);
            Message.MessageWrapper convertedResult = this.msgToGrpcConverter.convert(result);
            responseObserver.onNext(convertedResult);
            responseObserver.onCompleted();
        } catch (final Exception e) {
            log.error("Exception in handleVoteRequest()", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void handleLogRequest(Message.MessageWrapper request, StreamObserver<Message.MessageWrapper> responseObserver) {
        try {
            com.ozgen.jraft.model.Message message = this.grpcToMsgConverter.convert(request);
            com.ozgen.jraft.model.Message result = this.messageHandlerService.handleLogRequest(message);
            Message.MessageWrapper convertedResult = this.msgToGrpcConverter.convert(result);
            responseObserver.onNext(convertedResult);
            responseObserver.onCompleted();
        } catch (final Exception e) {
            log.error("Exception in handleLogRequest()", e);
            responseObserver.onError(e);
        }
    }
}
