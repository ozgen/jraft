package com.ozgen.jraft.model.converter;

import com.jraft.Message;
import com.ozgen.jraft.model.LogEntry;
import com.ozgen.jraft.model.payload.LogRequestPayload;
import com.ozgen.jraft.model.payload.LogResponsePayload;
import com.ozgen.jraft.model.payload.VoteRequestPayload;
import com.ozgen.jraft.model.payload.VoteResponsePayload;
import com.ozgen.jraft.model.payload.impl.LogRequestPayloadData;

import java.util.List;
import java.util.stream.Collectors;

public class MsgToGrpcConverter {

    // Convert custom message to gRPC MessageWrapper
    public Message.MessageWrapper convert(com.ozgen.jraft.model.Message customMessage) {
        Message.MessageWrapper.Builder grpcMessageWrapperBuilder = Message.MessageWrapper.newBuilder();

        grpcMessageWrapperBuilder.setSender(customMessage.getSender());
        grpcMessageWrapperBuilder.setTerm(customMessage.getTerm());

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

    private Message.VoteRequest convertVoteRequest(VoteRequestPayload voteRequestPayload) {
        return Message.VoteRequest.newBuilder()
                .setLastTerm(voteRequestPayload.getLastTerm())
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
                .setPrefixTerm(logRequestPayloadData.getPrefixTerm())
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
                .setMessage(convert(logEntry.getMessage()))
                .setTerm(logEntry.getTerm())
                .build();
    }
}
