package com.ozgen.jraft.service;

import com.ozgen.jraft.model.message.Message;
import com.ozgen.jraft.node.NodeServer;

import java.util.concurrent.CompletableFuture;

public interface MessageHandlerService {

  /**
   * Handles an incoming vote request message.
   *
   * @param message The vote request message to be processed.
   * @return The resulting message after processing the vote request.
   */
  CompletableFuture<Message> handleVoteRequest(Message message);

  /**
   * Handles an incoming log request message.
   *
   * @param message The log request message to be processed.
   * @return The resulting message after processing the log request.
   */
  CompletableFuture<Message> handleLogRequest(Message message);


  void setNodeServer(NodeServer nodeServer);
}

