package com.ozgen.jraft.node;

import com.jraft.Message;
import com.jraft.MessageHandlerServiceGrpc;
import com.ozgen.jraft.model.node.NodeData;
import com.ozgen.jraft.model.node.NodeResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DefaultNodeServer {

    private static final Logger log = LoggerFactory.getLogger(DefaultNodeServer.class);

    private ConcurrentHashMap<String, NodeData> nodes = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, ManagedChannel> channelPool = new ConcurrentHashMap<>();


    // Called when a node wants to join the cluster
    public CompletableFuture<NodeResponse> joinCluster(String newNodeId, NodeData nodeData) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Request to join the cluster received from node: {}", newNodeId);

            if (!nodes.containsKey(newNodeId)) {
                nodes.put(newNodeId, nodeData);
                log.info("Node {} joined the cluster.", newNodeId);
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
}
