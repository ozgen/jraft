package com.ozgen.jraft.model.node;

public class NodeResponse {

    private boolean success;

    private String message;


    public NodeResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
