package com.ozgen.jraft.model.converter;

import com.google.protobuf.Timestamp;
import com.jraft.Message;
import com.ozgen.jraft.model.LogEntry;
import com.ozgen.jraft.model.Term;
import com.ozgen.jraft.model.payload.VoteRequestPayload;
import com.ozgen.jraft.model.payload.impl.LogRequestPayloadData;
import com.ozgen.jraft.model.payload.impl.LogResponsePayloadData;
import com.ozgen.jraft.model.payload.impl.VoteRequestPayloadData;
import com.ozgen.jraft.model.payload.impl.VoteResponsePayloadData;

import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;

public class GrpcToMsgConverter {

    // Convert gRPC MessageWrapper to custom message
    public com.ozgen.jraft.model.Message convert(Message.MessageWrapper grpcMessageWrapper) {
        String sender = grpcMessageWrapper.getSender();
        Term term = new Term(grpcMessageWrapper.getTerm().getTerm());

        if (grpcMessageWrapper.hasVoteRequest()) {
            VoteRequestPayload voteRequestPayload = this.convertVoteRequest(grpcMessageWrapper.getVoteRequest());
            return new com.ozgen.jraft.model.Message(sender, term, voteRequestPayload);
        } else if (grpcMessageWrapper.hasVoteResponse()) {
            Message.VoteResponse voteResponse = grpcMessageWrapper.getVoteResponse();
            return new com.ozgen.jraft.model.Message(sender, term, voteResponse);
        } else if (grpcMessageWrapper.hasLogRequest()) {
            LogRequestPayloadData logRequestPayloadData = this.convertLogRequest(grpcMessageWrapper.getLogRequest());
            return new com.ozgen.jraft.model.Message(sender, term, logRequestPayloadData);
        } else if (grpcMessageWrapper.hasLogResponse()) {
            Message.LogResponse logResponse = grpcMessageWrapper.getLogResponse();
            return new com.ozgen.jraft.model.Message(sender, term, logResponse);

        } else {
            throw new IllegalArgumentException("Unsupported payload type in MessageWrapper");
        }
    }

    public Instant timestampToInstant(final Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private VoteRequestPayload convertVoteRequest(Message.VoteRequest voteRequest) {
        Term lastTerm = new Term(voteRequest.getLastTerm().getTerm());
        int logLength = voteRequest.getLogLength();

        return new VoteRequestPayloadData(logLength, lastTerm);
    }

    private LogRequestPayloadData convertLogRequest(Message.LogRequest logRequest) {
        CopyOnWriteArrayList<LogEntry> suffix = new CopyOnWriteArrayList<>();
        if (!logRequest.getSuffixList().isEmpty()) {
            for (Message.LogEntry logEntry : logRequest.getSuffixList()) {
                LogEntry entry = convertLogEntry(logEntry);
                suffix.add(entry);
            }
        }
        Term term = new Term(logRequest.getPrefixTerm().getTerm());
        return new LogRequestPayloadData(logRequest.getPrefixLength(), term, logRequest.getLeaderCommit(),
                suffix, logRequest.getLeaderId());
    }

    private com.ozgen.jraft.model.LogEntry convertLogEntry(Message.LogEntry logEntry) {
        com.ozgen.jraft.model.Message message = this.convert(logEntry.getMessage());
        return new LogEntry(new Term(logEntry.getTerm().getTerm()), message);
    }

    private com.ozgen.jraft.model.Message convert(Message.MessageContent grpcMessageContent) {
        String sender = grpcMessageContent.getSender();
        Term term = new Term(grpcMessageContent.getTerm().getTerm());

        if (grpcMessageContent.hasVoteRequest()) {
            VoteRequestPayload voteRequestPayload = this.convertVoteRequest(grpcMessageContent.getVoteRequest());
            return new com.ozgen.jraft.model.Message(sender, term, voteRequestPayload);
        } else if (grpcMessageContent.hasVoteResponse()) {
            Message.VoteResponse voteResponse = grpcMessageContent.getVoteResponse();
            return new com.ozgen.jraft.model.Message(sender, term, voteResponse);
        } else if (grpcMessageContent.hasLogRequest()) {
            LogRequestPayloadData logRequestPayloadData = this.convertLogRequest(grpcMessageContent.getLogRequest());
            return new com.ozgen.jraft.model.Message(sender, term, logRequestPayloadData);
        } else if (grpcMessageContent.hasLogResponse()) {
            Message.LogResponse logResponse = grpcMessageContent.getLogResponse();
            return new com.ozgen.jraft.model.Message(sender, term, logResponse);

        } else {
            throw new IllegalArgumentException("Unsupported payload type in MessageWrapper");
        }
    }
}
