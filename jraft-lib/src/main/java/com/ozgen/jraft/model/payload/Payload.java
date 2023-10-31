package com.ozgen.jraft.model.payload;

public interface Payload extends VoteRequestPayload, VoteResponsePayload, LogRequestPayload, LogResponsePayload {
    /**
     * This interface serves as a marker interface for payloads used in the Raft algorithm.
     * It extends other payload interfaces related to Vote Request, Vote Response, Log Request, and Log Response.
     * By implementing this interface, a class signifies that it represents a payload used in Raft communication.
     */
}
