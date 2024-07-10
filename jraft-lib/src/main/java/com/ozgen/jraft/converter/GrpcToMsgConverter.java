package com.ozgen.jraft.converter;

import com.google.protobuf.Timestamp;
import com.jraft.Message;
import com.ozgen.jraft.model.message.LogEntry;
import com.ozgen.jraft.model.message.Term;
import com.ozgen.jraft.model.message.payload.VoteRequestPayload;
import com.ozgen.jraft.model.message.payload.impl.LogRequestPayloadData;
import com.ozgen.jraft.model.message.payload.impl.VoteRequestPayloadData;
import com.ozgen.jraft.model.message.payload.impl.VoteResponsePayloadData;
import com.ozgen.jraft.model.node.Node;
import com.ozgen.jraft.model.node.NodeData;
import com.ozgen.jraft.model.node.NodeResponse;

import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;

public class GrpcToMsgConverter {

    // Convert gRPC MessageWrapper to custom message
    public com.ozgen.jraft.model.message.Message convertMessage(Message.MessageWrapper grpcMessageWrapper) {
        String sender = grpcMessageWrapper.getSender();
        Instant instant = timestampToInstant(grpcMessageWrapper.getTerm().getCreatedAt());
        Term term = new Term(grpcMessageWrapper.getTerm().getTerm(), instant);

        if (grpcMessageWrapper.hasVoteRequest()) {
            VoteRequestPayload voteRequestPayload = this.convertVoteRequest(grpcMessageWrapper.getVoteRequest());
            return new com.ozgen.jraft.model.message.Message(sender, term, voteRequestPayload);
        } else if (grpcMessageWrapper.hasVoteResponse()) {
            Message.VoteResponse voteResponse = grpcMessageWrapper.getVoteResponse();
            VoteResponsePayloadData voteResponsePayloadData = new VoteResponsePayloadData(voteResponse.getGranted());
            return new com.ozgen.jraft.model.message.Message(sender, term, voteResponsePayloadData);
        } else if (grpcMessageWrapper.hasLogRequest()) {
            LogRequestPayloadData logRequestPayloadData = this.convertLogRequest(grpcMessageWrapper.getLogRequest());
            return new com.ozgen.jraft.model.message.Message(sender, term, logRequestPayloadData);
        } else if (grpcMessageWrapper.hasLogResponse()) {
            Message.LogResponse logResponse = grpcMessageWrapper.getLogResponse();
            return new com.ozgen.jraft.model.message.Message(sender, term, logResponse);

        } else {
            throw new IllegalArgumentException("Unsupported payload type in MessageWrapper");
        }
    }

    public Node convertJoinRequest(jraft.Node.JoinRequest joinRequest) {
        String id = joinRequest.getNodeId();
        NodeData nodeData = this.convertNodeData(joinRequest.getNodeData());
        return new Node(id, nodeData);
    }

    public NodeResponse convertNodeResponse(jraft.Node.NodeResponse nodeResponse) {
        boolean success = nodeResponse.getSuccess();
        String message = nodeResponse.getMessage();
        return new NodeResponse(success, message);
    }

    public String convertLeaveRequest(jraft.Node.LeaveRequest leaveRequest) {
        return leaveRequest.getNodeId();
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

    private com.ozgen.jraft.model.message.LogEntry convertLogEntry(Message.LogEntry logEntry) {
        com.ozgen.jraft.model.message.Message message = this.convertMessage(logEntry.getMessage());
        return new LogEntry(new Term(logEntry.getTerm().getTerm()), message);
    }

    private com.ozgen.jraft.model.message.Message convertMessage(Message.MessageContent grpcMessageContent) {
        String sender = grpcMessageContent.getSender();
        Term term = new Term(grpcMessageContent.getTerm().getTerm());

        if (grpcMessageContent.hasVoteRequest()) {
            VoteRequestPayload voteRequestPayload = this.convertVoteRequest(grpcMessageContent.getVoteRequest());
            return new com.ozgen.jraft.model.message.Message(sender, term, voteRequestPayload);
        } else if (grpcMessageContent.hasVoteResponse()) {
            Message.VoteResponse voteResponse = grpcMessageContent.getVoteResponse();
            return new com.ozgen.jraft.model.message.Message(sender, term, voteResponse);
        } else if (grpcMessageContent.hasLogRequest()) {
            LogRequestPayloadData logRequestPayloadData = this.convertLogRequest(grpcMessageContent.getLogRequest());
            return new com.ozgen.jraft.model.message.Message(sender, term, logRequestPayloadData);
        } else if (grpcMessageContent.hasLogResponse()) {
            Message.LogResponse logResponse = grpcMessageContent.getLogResponse();
            return new com.ozgen.jraft.model.message.Message(sender, term, logResponse);

        } else {
            throw new IllegalArgumentException("Unsupported payload type in MessageWrapper");
        }
    }

    private NodeData convertNodeData(jraft.Node.NodeData nodeData) {
        //todo add ip address validaton...
        String ipAddress = nodeData.getIpAddress();
        int port = nodeData.getPort();
        return new NodeData(ipAddress, port);
    }
}
