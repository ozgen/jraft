package com.ozgen.jraft.model.converter;

import com.google.protobuf.Timestamp;
import com.jraft.Message;
import com.ozgen.jraft.model.LogEntry;
import com.ozgen.jraft.model.payload.VoteRequestPayload;
import com.ozgen.jraft.model.payload.impl.LogRequestPayloadData;
import com.ozgen.jraft.model.payload.impl.LogResponsePayloadData;
import com.ozgen.jraft.model.payload.impl.VoteRequestPayloadData;
import com.ozgen.jraft.model.payload.impl.VoteResponsePayloadData;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GrpcToMsgConverter {

    // Convert gRPC MessageWrapper to custom message
    public com.ozgen.jraft.model.Message convert(Message.MessageWrapper grpcMessageWrapper) {
        int sender = grpcMessageWrapper.getSender();
        int term = grpcMessageWrapper.getTerm();
        Instant createdAt = this.timestampToInstant(grpcMessageWrapper.getCreatedAt());

        if (grpcMessageWrapper.hasVoteRequest()) {
            VoteRequestPayload voteRequestPayload = this.convertVoteRequest(grpcMessageWrapper.getVoteRequest());
            return com.ozgen.jraft.model.Message.buildWithVoteForRequestPayload()
                    .sender(sender)
                    .term(term)
                    .createdAt(createdAt)
                    .voteRequestPayload(voteRequestPayload)
                    .build();
        } else if (grpcMessageWrapper.hasVoteResponse()) {
            Message.VoteResponse voteResponse = grpcMessageWrapper.getVoteResponse();
            return com.ozgen.jraft.model.Message.buildWithVoteResponsePayload()
                    .sender(sender)
                    .term(term)
                    .createdAt(createdAt)
                    .voteResponsePayload(VoteResponsePayloadData.builder()
                            .granted(voteResponse.getGranted())
                            .build())
                    .build();
        } else if (grpcMessageWrapper.hasLogRequest()) {
            LogRequestPayloadData logRequestPayloadData = this.convertLogRequest(grpcMessageWrapper.getLogRequest());
            return com.ozgen.jraft.model.Message.buildWithLogRequestPayload()
                    .term(term)
                    .sender(sender)
                    .createdAt(createdAt)
                    .logRequestPayload(logRequestPayloadData)
                    .build();
        } else if (grpcMessageWrapper.hasLogResponse()) {
            Message.LogResponse logResponse = grpcMessageWrapper.getLogResponse();
            return com.ozgen.jraft.model.Message.buildWithLogResponsePayload()
                    .term(term)
                    .sender(sender)
                    .createdAt(createdAt)
                    .logResponsePayload(LogResponsePayloadData.builder()
                            .ack(logResponse.getAck())
                            .granted(logResponse.getGranted())
                            .build())
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported payload type in MessageWrapper");
        }
    }

    public Instant timestampToInstant(final Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private VoteRequestPayload convertVoteRequest(Message.VoteRequest voteRequest) {
        int lastTerm = voteRequest.getLastTerm();
        int logLength = voteRequest.getLogLength();

        return VoteRequestPayloadData.builder()
                .lastTerm(lastTerm)
                .logLength(logLength)
                .build();
    }

    private LogRequestPayloadData convertLogRequest(Message.LogRequest logRequest) {
        List<LogEntry> suffix = new ArrayList<>();
        for (Message.LogEntry logEntry : logRequest.getSuffixList()) {
            LogEntry entry = convertLogEntry(logEntry);
            suffix.add(entry);
        }

        return LogRequestPayloadData.builder()
                .prefixLength(logRequest.getPrefixLength())
                .prefixTerm(logRequest.getPrefixTerm())
                .leaderCommit(logRequest.getLeaderCommit())
                .leaderId(logRequest.getLeaderId())
                .suffix(suffix)
                .build();
    }

    private com.ozgen.jraft.model.LogEntry convertLogEntry(Message.LogEntry logEntry) {
        return com.ozgen.jraft.model.LogEntry.builder()
                .message(this.convert(logEntry.getMessage()))
                .term(logEntry.getTerm())
                .build();
    }
}
