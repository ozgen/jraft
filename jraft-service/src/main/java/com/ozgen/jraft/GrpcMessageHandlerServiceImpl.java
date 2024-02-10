package com.ozgen.jraft;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jraft.Message;
import com.jraft.MessageHandlerServiceGrpc;
import com.ozgen.jraft.converter.GrpcToMsgConverter;
import com.ozgen.jraft.converter.MsgToGrpcConverter;
import com.ozgen.jraft.node.NodeServer;
import com.ozgen.jraft.service.MessageHandlerService;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class GrpcMessageHandlerServiceImpl extends MessageHandlerServiceGrpc.MessageHandlerServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(GrpcMessageHandlerServiceImpl.class);
    private final GrpcToMsgConverter grpcToMsgConverter;
    private final MsgToGrpcConverter msgToGrpcConverter;
    private final MessageHandlerService messageHandlerService;

    @Inject
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
            com.ozgen.jraft.model.message.Message message = this.grpcToMsgConverter.convertMessage(request);
            this.messageHandlerService.handleVoteRequest(message).whenComplete((result, t) -> {

                if (t != null) {
                    log.warn("Exception in handleVoteRequest()", t);
                    responseObserver.onError(t);
                }

                Message.MessageWrapper convertedResult = this.msgToGrpcConverter.convert(result);
                responseObserver.onNext(convertedResult);
                responseObserver.onCompleted();
            });
        } catch (final Exception e) {
            log.error("Exception in handleVoteRequest()", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void handleLogRequest(Message.MessageWrapper request, StreamObserver<Message.MessageWrapper> responseObserver) {
        try {
            com.ozgen.jraft.model.message.Message message = this.grpcToMsgConverter.convertMessage(request);
            this.messageHandlerService.handleLogRequest(message).whenComplete((result, t) -> {

                if (t != null) {
                    log.warn("Exception in handleLogRequest()", t);
                    responseObserver.onError(t);
                }

                Message.MessageWrapper convertedResult = this.msgToGrpcConverter.convert(result);
                responseObserver.onNext(convertedResult);
                responseObserver.onCompleted();
            });
        } catch (final Exception e) {
            log.error("Exception in handleLogRequest()", e);
            responseObserver.onError(e);
        }
    }

    public void setNodeServer(NodeServer nodeServer) {
        this.messageHandlerService.setNodeServer(nodeServer);
    }
}
