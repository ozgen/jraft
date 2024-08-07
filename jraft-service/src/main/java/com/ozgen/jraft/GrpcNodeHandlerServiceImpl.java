package com.ozgen.jraft;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ozgen.jraft.converter.GrpcToMsgConverter;
import com.ozgen.jraft.converter.MsgToGrpcConverter;
import com.ozgen.jraft.node.NodeServer;
import com.ozgen.jraft.service.NodeHandlerService;
import io.grpc.stub.StreamObserver;
import jraft.Node;
import jraft.NodeServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class GrpcNodeHandlerServiceImpl extends NodeServiceGrpc.NodeServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcNodeHandlerServiceImpl.class);
    private final GrpcToMsgConverter grpcToMsgConverter;
    private final MsgToGrpcConverter msgToGrpcConverter;
    private final NodeHandlerService nodeHandlerService;

    @Inject
    public GrpcNodeHandlerServiceImpl(GrpcToMsgConverter grpcToMsgConverter, MsgToGrpcConverter msgToGrpcConverter, NodeHandlerService nodeHandlerService) {
        this.grpcToMsgConverter = grpcToMsgConverter;
        this.msgToGrpcConverter = msgToGrpcConverter;
        this.nodeHandlerService = nodeHandlerService;
    }

    @Override
    public void joinCluster(Node.JoinRequest request, StreamObserver<Node.NodeResponse> responseObserver) {
        try {
            com.ozgen.jraft.model.node.Node node = this.grpcToMsgConverter.convertJoinRequest(request);
            this.nodeHandlerService.joinCluster(node).whenComplete((result, t) -> {

                if (t != null) {
                    log.warn("Exception in joinCluster()", t);
                    responseObserver.onError(t);
                }

                Node.NodeResponse nodeResponse = this.msgToGrpcConverter.convertNodeResponse(result);
                responseObserver.onNext(nodeResponse);
                responseObserver.onCompleted();
            });
        } catch (final Exception e) {
            log.error("Exception in joinCluster()", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void leaveCluster(Node.LeaveRequest request, StreamObserver<Node.NodeResponse> responseObserver) {
        try {
            String nodeId = this.grpcToMsgConverter.convertLeaveRequest(request);
            this.nodeHandlerService.leaveCluster(nodeId).whenComplete((result, t) -> {

                if (t != null) {
                    log.warn("Exception in leaveCluster()", t);
                    responseObserver.onError(t);
                }

                Node.NodeResponse nodeResponse = this.msgToGrpcConverter.convertNodeResponse(result);
                responseObserver.onNext(nodeResponse);
                responseObserver.onCompleted();
            });
        } catch (final Exception e) {
            log.error("Exception in leaveCluster()", e);
            responseObserver.onError(e);
        }
    }

    public void setNodeServer(NodeServer nodeServer) {
        this.nodeHandlerService.setNodeServer(nodeServer);
    }
}
