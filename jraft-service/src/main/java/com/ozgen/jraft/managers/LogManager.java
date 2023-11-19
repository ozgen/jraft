package com.ozgen.jraft.managers;

import com.ozgen.jraft.model.message.LogEntry;
import com.ozgen.jraft.model.message.Message;
import com.ozgen.jraft.model.NodeState;
import com.ozgen.jraft.model.message.Term;
import com.ozgen.jraft.converter.GrpcToMsgConverter;
import com.ozgen.jraft.converter.MsgToGrpcConverter;
import com.ozgen.jraft.model.enums.Role;
import com.ozgen.jraft.model.message.payload.LogRequestPayload;
import com.ozgen.jraft.model.message.payload.LogResponsePayload;
import com.ozgen.jraft.model.message.payload.impl.LogRequestPayloadData;
import com.ozgen.jraft.model.message.payload.impl.LogResponsePayloadData;
import com.ozgen.jraft.node.DefaultNodeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogManager extends DefaultNodeServer {
    private static final Logger log = LoggerFactory.getLogger(LogManager.class);


    private final MsgToGrpcConverter msgToGrpcConverter;
    private final GrpcToMsgConverter grpcToMsgConverter;


    public LogManager(  MsgToGrpcConverter msgToGrpcConverter, GrpcToMsgConverter grpcToMsgConverter) {
        this.msgToGrpcConverter = msgToGrpcConverter;
        this.grpcToMsgConverter = grpcToMsgConverter;
    }

    public CompletableFuture<Message> handleLogRequest(Message message, NodeState nodeState) {
        log.info("Received log request from sender: {}", message.getSender());

        LogRequestPayload payload = (LogRequestPayload) message.getPayload();
        return CompletableFuture.supplyAsync(() -> payload)
                .thenCompose(p -> {
                    log.debug("Handling log request with prefix length: {} and leader commit: {}", p.getPrefixLength(), p.getLeaderCommit());
                    return appendEntries(p.getPrefixLength(), p.getLeaderCommit(), p.getSuffixList(), nodeState);
                });
    }

    public CompletableFuture<Void> handleLogResponse(Message message,  ConcurrentHashMap<String, Integer> sentLength, ConcurrentHashMap<String, Integer> ackedLength, NodeState nodeState) {
        log.info("Received log response from sender: {}", message.getSender());

        return CompletableFuture.supplyAsync(() -> {
            String sender = message.getSender();
            Term term = message.getTerm();
            LogResponsePayload payload = (LogResponsePayload) message.getPayload();
            log.debug("Processing log response with term: {}, ack: {} and granted: {}", term, payload.getAck(), payload.isGranted());
            return new Object[]{sender, term, payload.getAck(), payload.isGranted()};
        }).thenCompose(data -> onLogResponseReceived(
                (String) data[0],
                (Term) data[1],
                (int) data[2],
                (boolean) data[3],
                sentLength,
                ackedLength,
                nodeState
        ));
    }


    private CompletableFuture<Void> replicateLog(String leaderId, String follower, ConcurrentHashMap<String, Integer> sentLength,ConcurrentHashMap<String, Integer> ackedLength, NodeState nodeState) {
        log.info("Initiating log replication from leader {} to follower {}.", leaderId, follower);
        return CompletableFuture.supplyAsync(() -> {
            if (!nodeState.getId().equals(leaderId)) {
                log.error("Node {} attempted log replication but it's not the leader. Only the leader {} should replicate logs.", nodeState.getId(), leaderId);
                throw new IllegalStateException("Only the leader should replicate logs");
            }

            // Create the log request
            int lastSent = sentLength.getOrDefault(follower, 0);
            List<LogEntry> entriesToSend = nodeState.getLogs().subList(lastSent, nodeState.getLogs().size());
            log.debug("Replicating {} log entries to follower {} starting from index {}.", entriesToSend.size(), follower, lastSent);

            LogRequestPayload payload = new LogRequestPayloadData(lastSent, nodeState.getCurrentTerm(), nodeState.getCommitLength(), new CopyOnWriteArrayList<>(entriesToSend), nodeState.getId());
            Message logRequestMessage = new Message(nodeState.getId(), nodeState.getCurrentTerm(), payload);

            return this.msgToGrpcConverter.convert(logRequestMessage);
        }).thenCompose(messageWrapper -> {
            log.debug("Sending log request to follower {}.", follower);
            return sendLogRequestToNode(follower, messageWrapper);
        }).thenApply(response -> {
            log.debug("Received response from follower {} after sending log request.", follower);
            return this.grpcToMsgConverter.convertMessage(response);
        }).thenCompose(responseMessage -> {
            log.debug("Handling log response from follower {}.", follower);
            return this.handleLogResponse(responseMessage, sentLength, ackedLength, nodeState);
        });
    }

    private CompletableFuture<Message> appendEntries(int prefixLen, int leaderCommit, CopyOnWriteArrayList<LogEntry> suffix, NodeState nodeState) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Appending entries. Prefix length: {}, Leader commit: {}, Suffix size: {}", prefixLen, leaderCommit, suffix.size());

            if (!suffix.isEmpty() && nodeState.getLogs().size() >= prefixLen) {
                int index = Math.min(nodeState.getLogs().size(), prefixLen + suffix.size()) - 1;
                if (!nodeState.getLogs().get(index).getTerm().equals(suffix.get(index - prefixLen).getTerm())) {
                    log.info("Mismatch detected. Truncating logs at prefix length: {}", prefixLen);
                    nodeState.setLogs(new CopyOnWriteArrayList<>(nodeState.getLogs().subList(0, prefixLen)));
                }
            }
            if (prefixLen + suffix.size() > nodeState.getLogs().size()) {
                log.info("Appending new entries from the suffix to the log.");
                for (int i = nodeState.getLogs().size() - prefixLen; i < suffix.size(); i++) {
                    nodeState.getLogs().add(suffix.get(i));
                }
            }
            return null;
        }).thenCompose(v -> {
            if (leaderCommit > nodeState.getCommitLength()) {
                log.info("Leader commit {} is greater than current commit length {}. Delivering log entries.", leaderCommit, nodeState.getCommitLength());
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = nodeState.getCommitLength(); i < leaderCommit; i++) {
                    futures.add(this.deliver(nodeState.getLogs().get(i).getMessage()));
                }
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .thenApply(ignored -> {
                             nodeState.setCommitLength(leaderCommit);
                            LogResponsePayload logResponsePayload = new LogResponsePayloadData(nodeState.getCommitLength(), true);
                            log.info("Log delivery completed. Sending log response with commit length: {}", nodeState.getCommitLength());
                            return new Message(nodeState.getId(), nodeState.getCurrentTerm(), logResponsePayload);
                        });
            } else {
                LogResponsePayload logResponsePayload = new LogResponsePayloadData(nodeState.getCommitLength(), true);
                log.debug("No need to deliver log entries. Sending log response with current commit length: {}", nodeState.getCommitLength());
                return CompletableFuture.completedFuture(new Message(nodeState.getId(), nodeState.getCurrentTerm(), logResponsePayload));
            }
        });
    }

    private CompletableFuture<Void> onLogResponseReceived(String follower, Term term, int ack, boolean success,
                                                          ConcurrentHashMap<String, Integer> sentLength, ConcurrentHashMap<String, Integer> ackedLength, NodeState nodeState) {
        return CompletableFuture.runAsync(() -> {
            log.info("Received log response from {}. Term: {}, Ack: {}, Success: {}", follower, term, ack, success);

            if (term.equals(nodeState.getCurrentTerm()) && nodeState.getCurrentRole() == Role.LEADER) {
                if (success && ack > ackedLength.getOrDefault(follower, 0)) {
                    log.info("Successfully replicated log to {}. Updating ack length.", follower);
                    sentLength.put(follower, ack);
                    ackedLength.put(follower, ack);
                    commitLogEntries(ackedLength, nodeState);  // logging inside this method might be beneficial too
                } else if (sentLength.getOrDefault(follower, 0) > 0) {
                    log.warn("Failed to replicate log to {}. Retrying...", follower);
                    sentLength.put(follower, sentLength.get(follower) - 1);
                    replicateLog(nodeState.getId(), follower,sentLength, ackedLength, nodeState);  // logging inside this method might be beneficial too
                }
            } else if (term.isGreaterThan(nodeState.getCurrentTerm())) {
                log.warn("Received log response with a higher term from {}. Reverting to follower role.", follower);
                nodeState.setCurrentTerm(term);
                nodeState.setCurrentRole(Role.FOLLOWER);
                nodeState.setVotedFor(null);
                // cancel election timer. Implement the timer cancellation logic here.
                // It might be useful to log the cancellation of the election timer too.
            } else {
                log.debug("Received outdated log response from {}. Ignoring.", follower);
            }
        });
    }


    private CompletableFuture<Void> commitLogEntries(ConcurrentHashMap<String, Integer> ackedLength, NodeState nodeState) {
        log.info("Initiating commit of log entries.");
        return CompletableFuture.runAsync(() -> {
            int commitLength = nodeState.getCommitLength();
            while (commitLength < nodeState.getLogs().size()) {
                int acks = 0;
                Set<String> nodes = this.getNodes();
                for (String node : nodes) {
                    if (ackedLength.getOrDefault(node, 0) > commitLength) {
                        acks++;
                    }
                }

                log.debug("Received acknowledgements for log entry at index {}: {}/{}.", commitLength, acks, nodes.size());

                if (acks > (nodes.size() + 1) / 2) {
                    log.info("Committing log entry at index {}.", commitLength);
                    deliver(nodeState.getLogs().get(commitLength).getMessage());
                    commitLength++;
                    nodeState.setCommitLength(commitLength);
                } else {
                    log.info("Not enough acknowledgements to commit log entry at index {}. Breaking out of commit loop.", commitLength);
                    break;
                }
            }
        });
    }

    private CompletableFuture<Void> deliver(Message message) {
        //method is expected to take a committed log entry and apply it
        // to whatever stateful system (state machine) the Raft protocol is safeguarding
        return null;
    }
}
