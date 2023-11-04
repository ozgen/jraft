package com.ozgen.jraft.communications;

import com.jraft.Message;

import java.io.Serializable;
import java.util.Set;

public interface GRPCluster<ID extends Serializable> {

    void broadcastVoteRequest(Message.MessageWrapper message);

    void broadcastLogRequest(Message.MessageWrapper message);

    boolean isQuorum(Set<ID> receivedVotes);
}
