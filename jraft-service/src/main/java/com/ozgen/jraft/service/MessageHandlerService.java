package com.ozgen.jraft.service;

import com.ozgen.jraft.model.Message;

public interface MessageHandlerService {

  /**
   * Handles an incoming vote request message.
   *
   * @param message The vote request message to be processed.
   * @return The resulting message after processing the vote request.
   */
  Message handleVoteRequest(Message message);

  /**
   * Handles an incoming log request message.
   *
   * @param message The log request message to be processed.
   * @return The resulting message after processing the log request.
   */
  Message handleLogRequest(Message message);
}

