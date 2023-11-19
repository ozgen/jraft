package com.ozgen.jraft.converter;

import com.google.protobuf.Timestamp;
import com.jraft.Message;
import com.ozgen.jraft.model.message.LogEntry;
import com.ozgen.jraft.model.message.payload.LogRequestPayload;
import com.ozgen.jraft.model.message.payload.LogResponsePayload;
import com.ozgen.jraft.model.message.payload.VoteRequestPayload;
import com.ozgen.jraft.model.message.payload.VoteResponsePayload;
import com.ozgen.jraft.model.message.payload.impl.LogRequestPayloadData;
import com.ozgen.jraft.model.node.NodeData;
import jraft.Node;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class MsgToGrpcConverter {

    // Convert custom message to gRPC MessageWrapper
    public Message.MessageWrapper convert(com.ozgen.jraft.model.message.Message customMessage) {
        Message.MessageWrapper.Builder grpcMessageWrapperBuilder = Message.MessageWrapper.newBuilder();

        grpcMessageWrapperBuilder.setSender(customMessage.getSender());
        grpcMessageWrapperBuilder.setTerm(Message.Term.newBuilder()
                .setTerm(customMessage.getTerm().getNumber())
                .build());

        Object payload = customMessage.getPayload();

        if (payload instanceof VoteRequestPayload) {
            grpcMessageWrapperBuilder.setVoteRequest(convertVoteRequest((VoteRequestPayload) payload));
        } else if (payload instanceof VoteResponsePayload) {
            VoteResponsePayload voteResponsePayload = (VoteResponsePayload) payload;
            grpcMessageWrapperBuilder.setVoteResponse(convertVoteResponse(voteResponsePayload.isGranted()));
        } else if (payload instanceof LogRequestPayload) {
            grpcMessageWrapperBuilder.setLogRequest(convertLogRequest((LogRequestPayloadData) payload));
        } else if (payload instanceof LogResponsePayload) {
            LogResponsePayload logResponsePayload = (LogResponsePayload) payload;
            grpcMessageWrapperBuilder.setLogResponse(convertLogResponse(logResponsePayload.isGranted(), logResponsePayload.getAck()));
        } else {
            throw new IllegalArgumentException("Unsupported payload type in custom Message");
        }

        return grpcMessageWrapperBuilder.build();
    }

    public Timestamp instantToTimestamp(final Instant instant) {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    public Node.JoinRequest convertJoinRequest(com.ozgen.jraft.model.node.Node node){
        return Node.JoinRequest.newBuilder()
                .setNodeId(node.getId())
                .setNodeData(this.convertNodeData(node.getNodeData()))
                .build();
    }

    public Node.NodeResponse convertNodeResponse(com.ozgen.jraft.model.node.NodeResponse nodeResponse){
        return Node.NodeResponse.newBuilder()
                .setSuccess(nodeResponse.isSuccess())
                .setMessage(nodeResponse.getMessage())
                .build();
    }

    public Node.LeaveRequest convertLeaveRequest(String nodeId){
        return Node.LeaveRequest.newBuilder()
                .setNodeId(nodeId)
                .build();
    }

    private Message.VoteRequest convertVoteRequest(VoteRequestPayload voteRequestPayload) {
        return Message.VoteRequest.newBuilder()
                .setLastTerm(Message.Term.newBuilder()
                        .setTerm(voteRequestPayload.getLastTerm().getNumber())
                        .build())
                .setLogLength(voteRequestPayload.getLogLength())
                .build();
    }

    private Message.VoteResponse convertVoteResponse(boolean granted) {
        return Message.VoteResponse.newBuilder()
                .setGranted(granted)
                .build();
    }

    private Message.LogRequest convertLogRequest(LogRequestPayloadData logRequestPayloadData) {
        List<Message.LogEntry> grpcLogEntries = logRequestPayloadData.getSuffixList().stream()
                .map(this::convertLogEntry)
                .collect(Collectors.toList());

        return Message.LogRequest.newBuilder()
                .setPrefixLength(logRequestPayloadData.getPrefixLength())
                .setPrefixTerm(Message.Term.newBuilder()
                        .setTerm(logRequestPayloadData.getPrefixTerm().getNumber())
                        .build())
                .setLeaderCommit(logRequestPayloadData.getLeaderCommit())
                .setLeaderId(logRequestPayloadData.getLeaderId())
                .addAllSuffix(grpcLogEntries)
                .build();
    }

    private Message.LogResponse convertLogResponse(boolean granted, int ack) {
        return Message.LogResponse.newBuilder()
                .setGranted(granted)
                .setAck(ack)
                .build();
    }

    private Message.LogEntry convertLogEntry(LogEntry logEntry) {
        return Message.LogEntry.newBuilder()
                .setMessage(convertContent(logEntry.getMessage()))
                .setTerm(Message.Term.newBuilder()
                        .setTerm(logEntry.getTerm().getNumber())
                        .build())
                .build();
    }

    private Message.MessageContent convertContent(com.ozgen.jraft.model.message.Message customMessage) {
        Message.MessageContent.Builder grpcMessageWrapperBuilder = Message.MessageContent.newBuilder();

        grpcMessageWrapperBuilder.setSender(customMessage.getSender());
        grpcMessageWrapperBuilder.setTerm(Message.Term.newBuilder()
                .setTerm(customMessage.getTerm().getNumber())
                .build());

        Object payload = customMessage.getPayload();

        if (payload instanceof VoteRequestPayload) {
            grpcMessageWrapperBuilder.setVoteRequest(convertVoteRequest((VoteRequestPayload) payload));
        } else if (payload instanceof VoteResponsePayload) {
            VoteResponsePayload voteResponsePayload = (VoteResponsePayload) payload;
            grpcMessageWrapperBuilder.setVoteResponse(convertVoteResponse(voteResponsePayload.isGranted()));
        } else if (payload instanceof LogRequestPayload) {
            grpcMessageWrapperBuilder.setLogRequest(convertLogRequest((LogRequestPayloadData) payload));
        } else if (payload instanceof LogResponsePayload) {
            LogResponsePayload logResponsePayload = (LogResponsePayload) payload;
            grpcMessageWrapperBuilder.setLogResponse(convertLogResponse(logResponsePayload.isGranted(), logResponsePayload.getAck()));
        } else {
            throw new IllegalArgumentException("Unsupported payload type in custom Message");
        }

        return grpcMessageWrapperBuilder.build();
    }

    private Node.NodeData convertNodeData(NodeData nodeData){
      return   Node.NodeData.newBuilder()
                .setIpAddress(nodeData.getIpAddress())
                .setPort(nodeData.getPort())
                .build();
    }
}
