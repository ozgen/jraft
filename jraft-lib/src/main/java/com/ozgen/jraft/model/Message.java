package com.ozgen.jraft.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class Message {

    private String sender;
    private Term term;
    private Object payload;

    private static final Logger logger =  LoggerFactory.getLogger(Message.class);

    // Vote Request Message Constructor
    public Message(String sender, Term term, Object payload) {
        this.sender = sender;
        this.term = term;
        this.payload = payload;
    }

    public String getSender() {
        return sender;
    }

    public Term getTerm() {
        return term;
    }

    public Object getPayload() {
        return payload;
    }
}
