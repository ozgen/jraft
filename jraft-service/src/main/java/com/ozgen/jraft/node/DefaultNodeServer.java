package com.ozgen.jraft.node;

import com.jraft.Message;
import com.jraft.MessageHandlerServiceGrpc;
import com.ozgen.jraft.converter.GrpcToMsgConverter;
import com.ozgen.jraft.converter.MsgToGrpcConverter;
import com.ozgen.jraft.model.node.Node;
import com.ozgen.jraft.model.node.NodeData;
import com.ozgen.jraft.model.node.NodeResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import jraft.NodeServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DefaultNodeServer {

    private static final Logger log = LoggerFactory.getLogger(DefaultNodeServer.class);

    protected ConcurrentHashMap<String, NodeData> nodes = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, ManagedChannel> channelPool = new ConcurrentHashMap<>();

    protected String id;

    protected MsgToGrpcConverter msgToGrpcConverter;
    protected GrpcToMsgConverter grpcToMsgConverter;


    // Called when a node wants to join the cluster
    public CompletableFuture<NodeResponse> joinCluster(String newNodeId, NodeData nodeData) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Request to join the cluster received from node: {}", newNodeId);

            if (!nodes.containsKey(newNodeId)) {
                nodes.put(newNodeId, nodeData);
                notifyAllJoinNodeToCluster(new Node(newNodeId, nodeData))
                        // todo notified value did not checked
                        .thenApply(notified -> CompletableFuture.completedFuture(null));

                log.info("Node {} joined the cluster IP: {}, PORT: {}.", newNodeId, nodeData.getIpAddress(), nodeData.getPort());
                // Optionally, broadcast this change to all nodes or use Raft itself to replicate this change
                return new NodeResponse(true, "Node " + newNodeId + " joined the cluster");
            } else {
                log.warn("Node {} is already a part of the cluster.", newNodeId);
                return new NodeResponse(false, "Node " + newNodeId + " is already part of the cluster");
            }
        });
    }

    // In leaveCluster method
    public CompletableFuture<NodeResponse> leaveCluster(String nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Request to leave the cluster received from node: {}", nodeId);

            if (nodes.containsKey(nodeId)) {
                this.removeNode(nodeId);
                nodes.remove(nodeId);
                notifyAllLeaveNodeToCluster(nodeId)
                        // todo notified value did not checked
                        .thenApply(notified -> CompletableFuture.completedFuture(null));
                log.info("Node {} left the cluster.", nodeId);
                // Optionally, broadcast this change to all nodes or use Raft itself to replicate this change
                return new NodeResponse(true, "Node " + nodeId + " left the cluster");
            } else {
                log.warn("Node {} is not part of the cluster.", nodeId);
                return new NodeResponse(false, "Node " + nodeId + " is not part of the cluster");
            }
        });
    }

    // Getter for other parts of your code
    public Set<String> getNodes() {
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public CompletableFuture<Message.MessageWrapper> sendVoteRequestToNode(String nodeId, Message.MessageWrapper message) {
        CompletableFuture<Message.MessageWrapper> futureResponse = new CompletableFuture<>();
        NodeData nodeData = nodes.get(nodeId);
        if (nodeData != null) {
            MessageHandlerServiceGrpc.MessageHandlerServiceStub client = createClientForNode(nodeData);
            client.handleVoteRequest(message, new StreamObserver<Message.MessageWrapper>() {
                @Override
                public void onNext(Message.MessageWrapper response) {
                    futureResponse.complete(response);
                }

                @Override
                public void onError(Throwable t) {
                    log.error("Error while sending vote request to node {}: {}", nodeId, t.getMessage());
                    futureResponse.completeExceptionally(t);
                }

                @Override
                public void onCompleted() {
                    // If there is no response until now, then the server finished the RPC without sending any response.
                    if (!futureResponse.isDone()) {
                        futureResponse.completeExceptionally(new RuntimeException("Server completed without sending a response"));
                    }
                }
            });
        } else {
            futureResponse.completeExceptionally(new RuntimeException("Node not found: " + nodeId));
        }
        return futureResponse;
    }


    public CompletableFuture<Message.MessageWrapper> sendLogRequestToNode(String nodeId, Message.MessageWrapper message) {
        CompletableFuture<Message.MessageWrapper> futureResponse = new CompletableFuture<>();
        NodeData nodeData = nodes.get(nodeId);
        if (nodeData != null) {
            MessageHandlerServiceGrpc.MessageHandlerServiceStub client = createClientForNode(nodeData);
            client.handleLogRequest(message, new StreamObserver<Message.MessageWrapper>() {
                @Override
                public void onNext(Message.MessageWrapper response) {
                    futureResponse.complete(response);
                }

                @Override
                public void onError(Throwable t) {
                    log.error("Error while sending log request to node {}: {}", nodeId, t.getMessage());
                    futureResponse.completeExceptionally(t);
                }

                @Override
                public void onCompleted() {
                    // If there is no response until now, then the server finished the RPC without sending any response.
                    if (!futureResponse.isDone()) {
                        futureResponse.completeExceptionally(new RuntimeException("Server completed without sending a response"));
                    }
                }
            });
        }
        return futureResponse;
    }

    private MessageHandlerServiceGrpc.MessageHandlerServiceStub createClientForNode(NodeData nodeData) {
        String key = nodeData.getIpAddress() + ":" + nodeData.getPort();
        log.debug("Creating client for node with IP: {} and port: {}", nodeData.getIpAddress(), nodeData.getPort());
        ManagedChannel channel = channelPool.computeIfAbsent(key, k ->
                ManagedChannelBuilder.forAddress(nodeData.getIpAddress(), nodeData.getPort())
                        .usePlaintext()
                        .build()
        );
        return MessageHandlerServiceGrpc.newStub(channel);
    }

    private NodeServiceGrpc.NodeServiceStub createNodeClientForNode(NodeData nodeData) {
        String key = nodeData.getIpAddress() + ":" + nodeData.getPort();
        log.debug("Creating client for node with IP: {} and port: {}", nodeData.getIpAddress(), nodeData.getPort());
        ManagedChannel channel = channelPool.computeIfAbsent(key, k ->
                ManagedChannelBuilder.forAddress(nodeData.getIpAddress(), nodeData.getPort())
                        .usePlaintext()
                        .build()
        );
        return NodeServiceGrpc.newStub(channel);
    }

    public void removeNode(String nodeId) {
        NodeData nodeData = nodes.get(nodeId);
        log.info("Removing node: {}", nodeId);
        if (nodeData != null) {
            String key = nodeData.getIpAddress() + ":" + nodeData.getPort();
            ManagedChannel channel = channelPool.remove(key);
            if (channel != null && !channel.isShutdown()) {
                log.debug("Initiating graceful shutdown for channel associated with node: {}", nodeId);
                channel.shutdown(); // Initiates a graceful shutdown
                try {
                    if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                        channel.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    channel.shutdownNow();
                }
            }
        }
    }

    public boolean checkServersAreReady() {
        return !nodes.isEmpty() && nodes.values().stream()
                .allMatch(nodeData -> isServerReady(nodeData.getIpAddress(), nodeData.getPort()));
    }


    private boolean isServerReady(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private CompletableFuture<Boolean> notifyAllJoinNodeToCluster(Node node) {
        List<CompletableFuture<jraft.Node.NodeResponse>> futures = new ArrayList<>();
        jraft.Node.JoinRequest joinRequest = msgToGrpcConverter.convertJoinRequest(node);
        getNodes().stream()
                .filter(nodeId -> !nodeId.equals(this.id)) // Exclude self
                .forEach(nodeId -> {
                    NodeData existingNodeData = nodes.get(nodeId);
                    futures.add(notifyOneJoinNodeToCluster(nodeId, existingNodeData, joinRequest));
                });

        // Wait for all the futures to complete and then return the overall status
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .allMatch(future -> {
                            try {
                                return future.get().getSuccess();
                            } catch (Exception e) {
                                return false;
                            }
                        }));
    }

    private CompletableFuture<jraft.Node.NodeResponse> notifyOneJoinNodeToCluster(String nodeId, NodeData existingNodeData, jraft.Node.JoinRequest joinRequest) {
        CompletableFuture<jraft.Node.NodeResponse> future = new CompletableFuture<>();
        NodeServiceGrpc.NodeServiceStub client = createNodeClientForNode(existingNodeData);
        client.joinCluster(joinRequest, new StreamObserver<jraft.Node.NodeResponse>() {
            @Override
            public void onNext(jraft.Node.NodeResponse nodeResponse) {
                future.complete(nodeResponse);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error while sending log request to node {}: {}", nodeId, t.getMessage());
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                if (!future.isDone()) {
                    future.completeExceptionally(new RuntimeException("Server completed without sending a response"));
                }
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> notifyAllLeaveNodeToCluster(String leftId) {
        List<CompletableFuture<jraft.Node.NodeResponse>> futures = new ArrayList<>();
        jraft.Node.LeaveRequest leaveRequest = msgToGrpcConverter.convertLeaveRequest(leftId);
        getNodes().stream()
                .filter(nodeId -> !nodeId.equals(this.id)) // Exclude self
                .forEach(nodeId -> {
                    NodeData existingNodeData = nodes.get(nodeId);
                    futures.add(notifyOneLeaveNodeFromCluster(nodeId, existingNodeData, leaveRequest));
                });

        // Wait for all the futures to complete and then return the overall status
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .allMatch(future -> {
                            try {
                                return future.get().getSuccess();
                            } catch (Exception e) {
                                return false;
                            }
                        }));
    }

    private CompletableFuture<jraft.Node.NodeResponse> notifyOneLeaveNodeFromCluster(String nodeId, NodeData existingNodeData, jraft.Node.LeaveRequest leaveRequest) {
        CompletableFuture<jraft.Node.NodeResponse> future = new CompletableFuture<>();
        NodeServiceGrpc.NodeServiceStub client = createNodeClientForNode(existingNodeData);
        client.leaveCluster(leaveRequest, new StreamObserver<jraft.Node.NodeResponse>() {
            @Override
            public void onNext(jraft.Node.NodeResponse nodeResponse) {
                future.complete(nodeResponse);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error while sending log request to node {}: {}", nodeId, t.getMessage());
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                if (!future.isDone()) {
                    future.completeExceptionally(new RuntimeException("Server completed without sending a response"));
                }
            }
        });
        return future;
    }
}
